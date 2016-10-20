package toolbox6.jartree.akka

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorAttributes, ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Keep, Sink, StreamConverters}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import toolbox6.jartree.servletapi.Processor

import scala.collection.JavaConversions
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

/**
  * Created by pappmar on 11/10/2016.
  */
abstract class AkkaProcessor(
  route: AkkaProcessor.Provider
) extends Processor with LazyLogging with AkkaProcessor.Input { self =>

  implicit val actorSystem = ActorSystem(
    self.getClass.getName.replace('.', '_'),
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
  implicit val materializer = ActorMaterializer()

  private val handler = Route.asyncHandler(route(this))

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
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

  override def close(): Unit = {
    actorSystem.shutdown()
    actorSystem.awaitTermination()
  }
}

object AkkaProcessor {

  trait Input {
    implicit def actorSystem : ActorSystem
    implicit def materializer : Materializer
  }

  type Provider = Input => Route

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
