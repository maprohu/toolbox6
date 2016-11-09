package toolbox6.modules

import mvnmod.builder.{ScalaModule, SubModuleContainer}
import toolbox6.modules.Toolbox6Modules.Logging


/**
  * Created by pappmar on 30/08/2016.
  */
object AkkaModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "akka")

  object Actor extends ScalaModule(
    "actor",
    Toolbox6Modules.Common.Snapshot,
    Toolbox6Modules.Logging.R1,
    mvn.`com.typesafe.akka:akka-actor_2.11:jar:2.3.15`
  )

  object Http extends ScalaModule(
    "http",
    mvn.`com.typesafe.akka:akka-http-experimental_2.11:jar:2.0.5`,
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
    mvn.`com.typesafe.akka:akka-slf4j_2.11:jar:2.3.15`
  ) {
    val Snapshot = snapshot
  }

  object Json4s extends ScalaModule(
    "json4s",
    mvn.`com.typesafe.akka:akka-http-experimental_2.11:jar:2.0.5`,
    mvn.`org.json4s:json4s-core_2.11:jar:3.4.0`,
    mvn.`org.json4s:json4s-native_2.11:jar:3.4.0`
  ) {
    val Snapshot = snapshot
  }

  object Stream extends ScalaModule(
    "stream",
    Actor,
    mvn.`com.typesafe.akka:akka-stream-experimental_2.11:jar:2.0.5`,
    mvn.`io.monix:monix-execution_2.11:jar:2.0.6`
  )


}
