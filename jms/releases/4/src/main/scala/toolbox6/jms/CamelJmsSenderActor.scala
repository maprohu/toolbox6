package toolbox6.jms

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Status}
import akka.actor.Actor.Receive
import akka.camel.{CamelExtension, Oneway, Producer}
import akka.event.Logging
import akka.util.Timeout

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Created by pappmar on 09/11/2016.
  */
class CamelJmsSenderActor(
  config : CamelJmsSenderActor.Config
) extends Actor with Producer with Oneway {
  import config._
  override def endpointUri: String = uri

  override protected def routeResponse(msg: Any): Unit = {
    sender() ! CamelJmsSenderAckActor.Ack
  }
}

object CamelJmsSenderActor {
  case class Config(
    uri: String
  )

}

class CamelJmsSenderAckActor(
  config: CamelJmsSenderAckActor.Config
) extends Actor {
  val log = Logging(context.system, this)
  import CamelJmsSenderAckActor._
  import config._

  val jmsSender = context.actorOf(
    Props(
      classOf[CamelJmsSenderActor],
      CamelJmsSenderActor.Config(
        uri = uri
      )
    )
  )

  var result : Try[Unit] = Success()

  override def receive: Receive = {
    case Pull =>
      (0 until bufferSize) foreach ( _ => sender() ! Ack )
    case f : Status.Failure =>
      result = Failure(f.cause)

      context stop self
    case Complete =>
      log.info("completing stream")

      val camel = CamelExtension(context.system)
      import context.dispatcher
      implicit val timeout : Timeout = 15.seconds
      for {
        _ <- camel.activationFutureFor(jmsSender)
        _ = jmsSender ! PoisonPill
        _ <- camel.deactivationFutureFor(jmsSender)
      } {
        self ! PoisonPill
      }

    case msg =>
      jmsSender forward msg
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    super.postStop()

    promise.tryComplete(result)

//    import context.dispatcher
//    val camel = CamelExtension(context.system)
//    implicit val timeout : Timeout = 10.seconds
//      camel
//        .deactivationFutureFor(self)
//        .onComplete({ r =>
//          println(r)
//          promise.tryComplete(result)
//        })
  }
}

object CamelJmsSenderAckActor {
  case class Config(
    uri: String,
    bufferSize: Int = 16,
    promise: Promise[Unit] = Promise()
  )
  case object Pull
  case object Ack
  case object Complete

}
