package toolbox6.akka.stream

import akka.actor.Actor
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

/**
  * Created by pappmar on 14/11/2016.
  */
class UnboundedSinkActor(
  config: UnboundedSinkActor.Config
) extends Actor {
  import config._
  implicit val materializer = ActorMaterializer.create(context.system)


  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()

  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    materializer.shutdown()
    super.postStop()
  }

}

object UnboundedSinkActor {
  case class Config(
    sink: Sink[Any, _]
  )
}
