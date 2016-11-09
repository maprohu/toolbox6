package toolbox6.jms

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Terminated}
import akka.camel.{CamelMessage, Consumer}
import toolbox6.jms.CamelJmsReceiverActor.Config

/**
  * Created by pappmar on 09/11/2016.
  */
class CamelJmsReceiverActor(
  config: Config
) extends Consumer {
  import config._

  override def endpointUri: String = uri

  override def preStart(): Unit = {
    super.preStart()

    context watch target
  }

  override def receive: Receive = {
    case msg: CamelMessage => target ! msg
    case _ : Terminated =>
      context stop self
  }
}
object CamelJmsReceiverActor {
  case class Config(
    uri: String,
    target: ActorRef
  )
}
