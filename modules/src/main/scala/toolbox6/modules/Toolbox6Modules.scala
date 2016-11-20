package toolbox6.modules

import mvnmod.builder.{MavenCentralModule, RootModuleContainer, ScalaModule}
import mvnmod.modules.MvnmodModules


/**
  * Created by martonpapp on 29/08/16.
  */
object Toolbox6Modules {

  implicit val Root = RootModuleContainer("toolbox6")

  object Modules extends ScalaModule(
    "modules",
    MvnmodModules.Modules.R5,
    MvnmodModules.Builder.R6
  ) {
    val Snapshot = snapshot

    object R6 extends Release(
      MvnmodModules.Modules.R5,
      MvnmodModules.Builder.R6
    )
    object R5 extends Release(
      MvnmodModules.Modules.R4,
      MvnmodModules.Builder.R5
    )
    object R4 extends Release(
      MvnmodModules.Modules.R3,
      MvnmodModules.Builder.R4
    )
    object R3 extends Release(
      MvnmodModules.Modules.R3,
      MvnmodModules.Builder.R4
    )
    object R2 extends Release(
      MvnmodModules.Modules.R2,
      MvnmodModules.Builder.R3
    )
    object R1 extends Release(
      MvnmodModules.Modules.R1,
      MvnmodModules.Builder.R2
    )
  }

  object Environment extends ScalaModule(
    "environment"
  ) {
    val Snapshot = snapshot

    object R1 extends Release(
    )
  }


  object Common extends ScalaModule(
    "common",
    Logging.R2,
    mvn.`commons-io:commons-io:jar:2.5`,
    mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
    mvn.`io.monix:monix_2.11:jar:2.0.6`,
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
  ) {
    val Snapshot = snapshot

    object R3 extends Release(
      Logging.R2,
      mvn.`commons-io:commons-io:jar:2.5`,
      mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
      mvn.`io.monix:monix_2.11:jar:2.0.6`,
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
    )
    object R2 extends Release(
      Logging.R1,
      mvn.`commons-io:commons-io:jar:2.5`,
      mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
      mvn.`io.monix:monix_2.11:jar:2.0.6`,
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
    )
    object R1 extends Release(
      Logging.R1,
      mvn.`commons-io:commons-io:jar:2.5`,
      mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
      mvn.`io.monix:monix_2.11:jar:2.0.4`,
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
    )
  }

  object Pickling extends ScalaModule(
    "pickling",
    Common.R3,
    mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
  ) {
    val Snapshot = snapshot

    object R3 extends Release(
      Common.R3,
      mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
    )
    object R2 extends Release(
      Common.R2,
      mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
    )
    object R1 extends Release(
      Common.R1,
      mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
    )
  }

  object Logback extends ScalaModule(
    "logback",
    mvn.`ch.qos.logback:logback-classic:jar:1.1.7`
  ) {
    val Snapshot = snapshot

    object R1 extends Release(
      mvn.`ch.qos.logback:logback-classic:jar:1.1.7`
    )
  }

  object Logging extends ScalaModule(
    "logging",
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
    mvn.`org.slf4j:jcl-over-slf4j:jar:1.7.21`
  ) {
    val Snapshot = snapshot

    object R2 extends Release(
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
      mvn.`org.slf4j:jcl-over-slf4j:jar:1.7.21`
    )
    object R1 extends Release(
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
    )
  }

  object Jms extends ScalaModule(
    "jms",
    AkkaModules.Stream.R2,
    mvn.`javax.jms:jms-api:jar:1.1-rev-1`,
    mvn.`io.monix:monix_2.11:jar:2.0.6`,
    mvn.`com.typesafe.akka:akka-stream-experimental_2.11:jar:2.0.5`,
    mvn.`com.typesafe.akka:akka-camel_2.11:jar:2.3.15`,
    mvn.`org.apache.camel:camel-jms:jar:2.13.4`,
    mvn.`org.apache.camel:camel-core:jar:2.13.4`
  ) {
    val Snapshot = snapshot

    object R3 extends Release(
      AkkaModules.Stream.R2,
      mvn.`javax.jms:jms-api:jar:1.1-rev-1`,
      mvn.`io.monix:monix_2.11:jar:2.0.6`,
      mvn.`com.typesafe.akka:akka-stream-experimental_2.11:jar:2.0.5`,
      mvn.`com.typesafe.akka:akka-camel_2.11:jar:2.3.15`,
      mvn.`org.apache.camel:camel-jms:jar:2.13.4`,
      mvn.`org.apache.camel:camel-core:jar:2.13.4`
    )
    object R2 extends Release(
      AkkaModules.Stream.R1,
      mvn.`javax.jms:jms-api:jar:1.1-rev-1`,
      mvn.`io.monix:monix_2.11:jar:2.0.6`,
      mvn.`com.typesafe.akka:akka-stream-experimental_2.11:jar:2.0.5`,
      mvn.`com.typesafe.akka:akka-camel_2.11:jar:2.3.15`,
      mvn.`org.apache.camel:camel-jms:jar:2.13.4`,
      mvn.`org.apache.camel:camel-core:jar:2.13.4`
    )
    object R1 extends Release(
      AkkaModules.Stream.R1,
      mvn.`javax.jms:jms-api:jar:1.1-rev-1`,
      mvn.`io.monix:monix_2.11:jar:2.0.6`,
      mvn.`com.typesafe.akka:akka-stream-experimental_2.11:jar:2.0.5`,
      mvn.`com.typesafe.akka:akka-camel_2.11:jar:2.3.15`,
      mvn.`org.apache.camel:camel-jms:jar:2.13.4`,
      mvn.`org.apache.camel:camel-core:jar:2.13.4`
    )
  }

  object Packaging extends ScalaModule(
    "packaging",
    MvnmodModules.Builder.R4,
    Pickling.R3,
    mvn.`org.scala-lang.modules:scala-xml_2.11:jar:1.0.6`,
    mvn.`org.apache.maven.shared:maven-invoker:2.2`,
    mvn.`org.scala-sbt:io_2.11:jar:1.0.0-M3`
//    mvn.`org.docx4j:docx4j:jar:3.3.1`,
//    mvn.`org.docx4j:docx4j-ImportXHTML:jar:3.3.1`
  ) {
//    object R1 extends Release(
//      MavenModulesBuilder,
//      Pickling.R1,
//      mvn.`org.scala-lang.modules:scala-xml_2.11:jar:1.0.6`,
//      mvn.`org.apache.maven.shared:maven-invoker:2.2`,
//      mvn.`org.scala-sbt:io_2.11:jar:1.0.0-M3`
//    )

  }

  object Macros extends ScalaModule(
    "macros",
    mvn.`org.scala-lang:scala-reflect:jar:2.11.8`
  ) {
    val Snapshot = snapshot

    object R1 extends Release(
      mvn.`org.scala-lang:scala-reflect:jar:2.11.8`
    )
  }

  object StateMachine extends ScalaModule(
    "statemachine",
    mvn.`io.monix:monix-reactive_2.11:jar:2.0.4`
  )

  object Docs extends ScalaModule(
    "docs",
    mvn.`com.lihaoyi:scalatex-site_2.11:jar:0.3.6`
  )



}




//object MavenModulesBuilder extends MavenCentralModule(
//  "maven-modules",
//  "maven-modules-builder",
//  "1.0.0"
//)
