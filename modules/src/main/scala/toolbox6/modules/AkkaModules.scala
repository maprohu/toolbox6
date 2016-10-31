package toolbox6.modules

import maven.modules.builder.{JavaModule, ScalaModule, SubModuleContainer}

/**
  * Created by pappmar on 30/08/2016.
  */
object AkkaModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "akka")

  object Http extends ScalaModule(
    "http",
    mvn.`com.typesafe.akka:akka-http-experimental_2.11:jar:2.0.5`,
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
    mvn.`com.typesafe.akka:akka-slf4j_2.11:2.3.15`
  ) {
//    object R2 extends Release(
//      mvn.`com.typesafe.akka:akka-http-experimental_2.11:jar:2.0.5`,
//      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
//      mvn.`com.typesafe.akka:akka-slf4j_2.11:2.3.15`
//    )
//    object R1 extends Release(
//      mvn.`com.typesafe.akka:akka-http-experimental_2.11:jar:2.0.5`,
//      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
//      mvn.`com.typesafe.akka:akka-slf4j_2.11:2.3.15`
//    )
  }

  object Json4s extends ScalaModule(
    "json4s",
    mvn.`com.typesafe.akka:akka-http-experimental_2.11:jar:2.0.5`,
    mvn.`org.json4s:json4s-core_2.11:jar:3.4.0`,
    mvn.`org.json4s:json4s-native_2.11:jar:3.4.0`
  ) {
//    object R1 extends Release(
//      mvn.`com.typesafe.akka:akka-http-experimental_2.11:jar:2.0.5`,
//      mvn.`org.json4s:json4s-core_2.11:jar:3.4.0`,
//      mvn.`org.json4s:json4s-native_2.11:jar:3.4.0`
//    )
  }


}
