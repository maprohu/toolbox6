package toolbox6.jartree.akka

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorAttributes, ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Keep, Sink, StreamConverters}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import toolbox6.common.HygienicThread
import toolbox6.jartree.servletapi.Processor
import toolbox6.logging.LogTools

import scala.collection.JavaConversions
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Try

/**
  * Created by pappmar on 11/10/2016.
  */
abstract class AkkaProcessor(
  route: AkkaProcessor.Provider
) extends Processor with LazyLogging with LogTools with AkkaProcessor.Input { self =>
  logger.info("creating akka processor")


  implicit val actorSystem = AkkaProcessor.createActorSystem(self)
  implicit val materializer = ActorMaterializer()

  private val (createdRoute, stopRoute) = route(this)
  private val handler = Route.asyncHandler(createdRoute)

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    HygienicThread.execute {
      import actorSystem.dispatcher
      try {
        Await.result(
          for {
            r <- handler(
              AkkaProcessor.wrapRequest(req)
            )
            u <- AkkaProcessor.unwrapResponse(
              r,
              resp
            )
          } yield {
            u
          },
          Duration.Inf
        )
      } catch {
        case ex : Throwable =>
          logger.error(ex.getMessage, ex)
          Try(resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
          Try(resp.setContentType("text/plain"))
          Try(resp.getWriter.print("ERROR"))
      }
    }
  }

  override def close(): Unit = {
    logger.info("closing akka processor")
    quietly { stopRoute() }
    quietly { Await.result(Http().shutdownAllConnectionPools(), 30.seconds) }
    quietly { actorSystem.shutdown() }
    quietly { actorSystem.awaitTermination() }
  }
}

object AkkaProcessor {
  def createActorSystem(self: Any) = ActorSystem(
    self.getClass.getSimpleName,
    ConfigFactory.parseString(
      """
        |akka {
        |  loggers = ["akka.event.slf4j.Slf4jLogger"]
        |  loglevel = "DEBUG"
        |  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
        |  jvm-exit-on-fatal-error = false
        |  actor {
        |    default-dispatcher {
        |      executor = "thread-pool-executor"
        |    }
        |  }
        |}
      """.stripMargin
    ).withFallback(ConfigFactory.load()),
    self.getClass.getClassLoader
  )

  trait Input {
    implicit val actorSystem : ActorSystem
    implicit val materializer : Materializer
  }

  type Provider = Input => (Route, () => Unit)

  def wrapRequest(req: HttpServletRequest) : HttpRequest = {
    import JavaConversions._

    val requestURI = req.getRequestURI.replaceAll("/+", "/")

    HttpRequest(
      method = HttpMethods.getForKey(req.getMethod).get,
      uri = Uri(requestURI).copy(rawQueryString = Option(req.getQueryString)),
      headers = req.getHeaderNames.asInstanceOf[java.util.Enumeration[String]].toIterable.map({ headerName =>
        HttpHeader.parse(headerName, req.getHeader(headerName)).asInstanceOf[Ok].header
      })(collection.breakOut),
      entity = HttpEntity(
        contentType =
          Option(req.getContentType)
            .map(ct => ContentType.parse(ct).right.get)
            .getOrElse(ContentTypes.`application/octet-stream`),
        data =
          StreamConverters
            .fromInputStream(() => req.getInputStream)
      ),
      protocol = HttpProtocols.getForKey(req.getProtocol).get
    )

  }

  def unwrapResponse(
    httpResponse: HttpResponse,
    res: HttpServletResponse
  )(implicit
    materializer: Materializer
  ) : Future[Any] = {
    httpResponse.headers.foreach { h =>
      res.setHeader(h.name(), h.value())
    }
    res.setStatus(httpResponse.status.intValue())
    res.setContentType(httpResponse.entity.contentType.toString())
    httpResponse.entity.contentLengthOption.foreach { cl =>
      res.setContentLength(cl.toInt)
    }

    httpResponse.entity.dataBytes
      .toMat(
        StreamConverters.fromOutputStream(
          () => res.getOutputStream
        )
      )(Keep.right)
      .run()
  }

}
