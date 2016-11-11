package toolbox6.jartree.testing

import akka.actor.{ActorSystem, FakeActorSystem}
import akka.http.scaladsl.server.RoutingSettings

/**
  * Created by pappmar on 11/11/2016.
  */
object RunFakeActorSystem {

  def main(args: Array[String]): Unit = {

    RoutingSettings(FakeActorSystem())
  }

}
