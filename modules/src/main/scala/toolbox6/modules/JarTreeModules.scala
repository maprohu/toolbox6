package toolbox6.modules

import mvnmod.builder.{MavenCentralModule, ScalaModule, SubModuleContainer}
import mvnmod.modules.MvnmodModules
/**
  * Created by pappmar on 04/11/2016.
  */
object JarTreeModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "jartree")

  object Api extends ScalaModule(
    "api"
  ) {
    val Snapshot = snapshot

    object R2 extends Release()
    object R1 extends Release()
  }

  object ServletApi extends ScalaModule(
    "servletapi",
    Api.R2,
    mvn.`javax.servlet:servlet-api:jar:2.5`
  ) {
    val Snapshot = snapshot

    object R2 extends Release(
      Api.R2,
      mvn.`javax.servlet:servlet-api:jar:2.5`
    )
    object R1 extends Release(
      Api.R1, mvn.`javax.servlet:servlet-api:jar:2.5`
    )
  }

  object Util extends ScalaModule(
    "util",
    Api.R2,
    mvn.`commons-io:commons-io:jar:2.5`,
    mvn.`commons-codec:commons-codec:jar:1.10`,
    mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`,
    Toolbox6Modules.Logging.R1
  ) {
    val Snapshot = snapshot

    object R2 extends Release(
      Api.R2,
      mvn.`commons-io:commons-io:jar:2.5`,
      mvn.`commons-codec:commons-codec:jar:1.10`,
      mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`,
      Toolbox6Modules.Logging.R1
    )
    object R1 extends Release(
      Api.R1,
      mvn.`commons-io:commons-io:jar:2.5`,
      mvn.`commons-codec:commons-codec:jar:1.10`,
      mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`,
      Toolbox6Modules.Logging.R1
    )
  }

  object Impl extends ScalaModule(
    "impl",
    MvnmodModules.Builder.R3,
    Api.R2,
    Util.R2,
    Toolbox6Modules.Logging.R1,
    Wiring.R2,
    Toolbox6Modules.Common.R2,
    Toolbox6Modules.Pickling.R2,
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
    mvn.`io.monix:monix-eval_2.11:jar:2.0.6`,
    mvn.`me.chrons:boopickle_2.11:jar:1.2.4`,
    mvn.`commons-io:commons-io:jar:2.5`
  ) {
    val Snapshot = snapshot

    object R2 extends Release(
      MvnmodModules.Builder.R3,
      Api.R2,
      Util.R2,
      Toolbox6Modules.Logging.R1,
      Wiring.R2,
      Toolbox6Modules.Common.R2,
      Toolbox6Modules.Pickling.R2,
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
      mvn.`io.monix:monix-eval_2.11:jar:2.0.6`,
      mvn.`me.chrons:boopickle_2.11:jar:1.2.4`,
      mvn.`commons-io:commons-io:jar:2.5`
    )
    object R1 extends Release(
      MvnmodModules.Builder.R2,
      Api.R1,
      Util.R1,
      Toolbox6Modules.Logging.R1,
      Wiring.R1,
      Toolbox6Modules.Common.R1,
      Toolbox6Modules.Pickling.R1,
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
      mvn.`io.monix:monix-eval_2.11:jar:2.0.5`,
      mvn.`me.chrons:boopickle_2.11:jar:1.2.4`,
      mvn.`commons-io:commons-io:jar:2.2`
    )
  }


  object Servlet extends ScalaModule(
    "servlet",
    Impl.R2,
    ServletApi.R2,
    Toolbox6Modules.Logging.R1,
    ManagementApi.R2,
    Wiring.R2,
    Toolbox6Modules.Common.R2,
    mvn.`io.monix:monix-execution_2.11:jar:2.0.6`,
    mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`
  ) {
    val Snapshot = snapshot

    object R2 extends Release(
      Impl.R2,
      ServletApi.R2,
      Toolbox6Modules.Logging.R1,
      ManagementApi.R2,
      Wiring.R2,
      Toolbox6Modules.Common.R2,
      mvn.`io.monix:monix-execution_2.11:jar:2.0.6`,
      mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`
    )
    object R1 extends Release(
      Impl.R1,
      ServletApi.R1,
      Toolbox6Modules.Logging.R1,
      ManagementApi.R1,
      Wiring.R1,
      Toolbox6Modules.Common.R1,
      mvn.`io.monix:monix-execution_2.11:jar:2.0.5`,
      mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`
    )
  }

  object Webapp extends ScalaModule(
    "webapp",
    Servlet.R1,
    mvn.`ch.qos.logback:logback-classic:jar:1.1.7`
  ) {
//    object R2 extends Release(
//      Servlet.R2,
//      mvn.`ch.qos.logback:logback-classic:jar:1.1.7`
//    )
//    object R1 extends Release(
//      Servlet.R1,
//      mvn.`ch.qos.logback:logback-classic:jar:1.1.7`
//    )
  }

  object Wiring extends ScalaModule(
    "wiring",
    ServletApi.R2,
    Util.R2,
    Toolbox6Modules.Logging.R1,
    mvn.`io.monix:monix_2.11:jar:2.0.6`,
    mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
    mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
  ) {
    val Snapshot = snapshot

    object R2 extends Release(
      ServletApi.R2,
      Util.R2,
      Toolbox6Modules.Logging.R1,
      mvn.`io.monix:monix_2.11:jar:2.0.6`,
      mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
      mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
    )
    object R1 extends Release(
      Api.R1,
      ServletApi.R1,
      Util.R1,
      Toolbox6Modules.Logging.R1,
      mvn.`io.monix:monix_2.11:jar:2.0.5`,
      mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
      mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
    )
  }


  object ManagementApi extends ScalaModule(
    "managementapi",
    Wiring.R2
  ) {
    val Snapshot = snapshot

    object R2 extends Release(
      Wiring.R2
    )
    object R1 extends Release(
      Wiring.R1
    )
  }

//  object ManagementUtils extends ScalaModule(
//    "managementutils",
//    ManagementApi,
//    Toolbox6Modules.Common,
//    Wiring,
//    ServletApi
//  ) {
////    object R2 extends Release(
////      ManagementApi.R2,
////      Toolbox6Modules.Common.R2,
////      Wiring.R2,
////      ServletApi.R2
////    )
////    object R1 extends Release(
////      ManagementApi.R1,
////      Toolbox6Modules.Common.R1
////    )
//  }

  object Client extends ScalaModule(
    "client",
    Toolbox6Modules.Modules.R1,
//    ManagementUtils,
    Packaging,
    mvn.`com.oracle:wlfullclient:jar:10.3.6.0`
  ) {
//    object R1 extends Release(
//      Toolbox6Modules.Modules,
//      ManagementUtils.R2,
//      Packaging.R1,
//      mvn.`com.oracle:wlfullclient:jar:10.3.6.0`
//    )
  }

  object Akka extends ScalaModule(
    "akka",
    ServletApi.R2,
    Toolbox6Modules.Common.R2,
    AkkaModules.Http.Snapshot,
    Toolbox6Modules.Logging.R1
  ) {
    val Snapshot = snapshot

//    object R2 extends Release(
//      ServletApi.R2,
//      Toolbox6Modules.Common.R2,
//      AkkaModules.Http.R2,
//      Toolbox6Modules.Logging.R2
//    )
//    object R1 extends Release(
//      ServletApi.R1,
//      AkkaModules.Http.R1
//    )
  }

  object Testing extends ScalaModule(
    "testing",
    Api.R2,
    ServletApi.R2,
    Packaging,
    Impl.R2,
    Toolbox6Modules.Common
  )

  object Packaging extends ScalaModule(
    "packaging",
    Toolbox6Modules.Modules.R1,
    Toolbox6Modules.Packaging,
    ServletApi.R1,
    Util.R1,
    Servlet.R1,
    mvn.`org.scala-lang.modules:scala-xml_2.11:jar:1.0.6`,
    mvn.`org.scala-sbt:io_2.11:jar:1.0.0-M3`,
    mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi-maven:jar:2.2.2`,
    mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi-maven:jar:2.2.2`,
    mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven:jar:2.2.2`,
    mvn.`com.oracle:wlfullclient:jar:10.3.6.0`
  ) {
//    object R1 extends Release(
//      Toolbox6Modules.Modules,
//      Toolbox6Modules.Packaging.R1,
//      ServletApi.R2,
//      Util.R2,
//      Servlet.R2,
//      mvn.`org.scala-lang.modules:scala-xml_2.11:jar:1.0.6`,
//      mvn.`org.scala-sbt:io_2.11:jar:1.0.0-M3`,
//      mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi-maven:jar:2.2.2`,
//      mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi-maven:jar:2.2.2`,
//      mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven:jar:2.2.2`,
//      mvn.`com.oracle:wlfullclient:jar:10.3.6.0`
//    )
  }

}
