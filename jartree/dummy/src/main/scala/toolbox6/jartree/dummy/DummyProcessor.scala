package toolbox6.jartree.dummy

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import akka.actor.FakeActorSystem
import akka.http.scaladsl.server.RoutingSettings
import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import toolbox6.akka.http.AkkaHttpTools
import toolbox6.jartree.api.{JarPlugResponse, JarPlugger, PullParams}
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.logging.LogTools

import scala.concurrent.Future

/**
  * Created by pappmar on 11/11/2016.
  */
class DummyProcessor extends Processor {
  override def close(): Unit = ()
  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
}
class DummyProcessorPlugger
  extends JarPlugger[Processor, JarTreeServletContext]
  with LazyLogging
  with LogTools
{
  override def pull(
    params: PullParams[Processor, JarTreeServletContext]
  ): Future[JarPlugResponse[Processor]] = {
    import params._
    AkkaHttpTools.clearCache

    val prevClose = Cancelable(() => previous.close())

    Future.successful(
      JarPlugResponse(
        new DummyProcessor,
        prevClose.cancel
      )
    )
  }
}
