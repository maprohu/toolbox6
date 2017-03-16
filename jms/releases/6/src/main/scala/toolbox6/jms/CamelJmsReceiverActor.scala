package toolbox6.jms

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Terminated}
import akka.camel.{CamelExtension, CamelMessage, Consumer}
import akka.event.Logging
import akka.util.Timeout
import org.apache.camel.{Exchange, Predicate}
import org.apache.camel.model.{ProcessorDefinition, RouteDefinition}
import toolbox6.jms.CamelJmsReceiverActor.Config

import scala.concurrent.Promise
import scala.concurrent.duration._

/**
  * Created by pappmar on 09/11/2016.
  */
class CamelJmsReceiverActor(
  config: Config
) extends Consumer {
  val log = Logging(context.system, this)
  import context.dispatcher
  import config._

  @volatile var stopping = false


  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.info(s"stopping jms receiver: ${config.uri}")
    stopping = true
    super.postStop()
  }

  override def onRouteDefinition: (RouteDefinition) => ProcessorDefinition[_] = { rd =>
    rd.filter(
      new Predicate {
        override def matches(exchange: Exchange): Boolean = {
          !stopping
        }
      }
    )
  }

  override def endpointUri: String = uri

  override def preStart(): Unit = {
    log.info(s"starting jms receiver: ${config.uri} - ref: ${self}")
    super.preStart()

    context watch target
  }

  override def receive: Receive = {
    case msg: CamelMessage => target ! msg
    case _ : Terminated =>
      log.info(s"target terminated: ${config.uri}")

      val camel = CamelExtension(context.system)
      implicit val timeout : Timeout = 10.seconds
      promise.tryCompleteWith(
        camel
          .deactivationFutureFor(self)
          .map(_ => ())
      )
      context stop self
//    case other =>
//      println(s"!!!!!!!!!!!!!!!! ${other}")
  }
}
object CamelJmsReceiverActor {
  case class Config(
    uri: String,
    target: ActorRef,
    promise : Promise[Unit] = Promise()
  )
}
