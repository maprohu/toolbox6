package akka.stream

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

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

  lazy val Debug = new Context {
    override implicit val actorSystem: ActorSystem = ActorSystem(
      "debugSystem",
      ConfigFactory
        .parseString(
          """
            |akka{
            |  loglevel = "DEBUG"
            |}
          """.stripMargin
        )
        .withFallback(ConfigFactory.load())
    )
    override implicit val materializer: Materializer = ActorMaterializer()
  }

}
