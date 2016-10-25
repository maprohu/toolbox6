package toolbox6.modules

import maven.modules.builder.{ScalaModule, SubModuleContainer}


/**
  * Created by pappmar on 02/09/2016.
  */
object ScalajsModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "scalajs")

  object Shared extends ScalaModule(
    "shared"
  )

  object Client extends ScalaModule(
    "client",
    Shared
  )

  object Server extends ScalaModule(
    "server",
    Shared,
    mvn.`com.typesafe.akka:akka-http-experimental_2.11:jar:2.0.5`,
    mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`
  )

  object Analyzer extends ScalaModule(
    "analyzer",
    mvn.`org.scala-lang:scala-reflect:jar:2.11.8`,
    mvn.`org.scala-js:scalajs-library_2.11:jar:0.6.12`,
    mvn.`org.scala-sbt:io_2.11:jar:1.0.0-M3`
  )

}

object FacadeModules {

  implicit val Container = SubModuleContainer(ScalajsModules.Container, "facades")

}

object VisModules {

  implicit val Container = SubModuleContainer(FacadeModules.Container, "vis")

  object Raw extends ScalaModule(
    "raw",
    mvn.`org.scala-js:scalajs-library_2.11:jar:0.6.12`
  )

}
