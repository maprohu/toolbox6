package akka.stream

import akka.actor.ActorSystem

/**
  * Created by pappmar on 15/11/2016.
  */
object AkkaStreamTools {

  trait Context {
    implicit val actorSystem : ActorSystem
    implicit val materializer : Materializer
    implicit def dispatcher = actorSystem.dispatcher
  }


  lazy val Default = new Context {
    override implicit val actorSystem: ActorSystem = ActorSystem()
    override implicit val materializer: Materializer = ActorMaterializer()
  }

}
