package toolbox6.modules

import maven.modules.builder._
import maven.modules.utils.MavenCentralModule
import toolbox6.modules.Toolbox6Modules.{Common, Logging}


/**
  * Created by martonpapp on 29/08/16.
  */
object Toolbox6Modules extends MavenCentralModule(
  "toolbox6-modules",
  "toolbox6-modules",
  "1.0.0-SNAPSHOT"
) {

  implicit val Root = RootModuleContainer("toolbox6")

  object Environment extends ScalaModule(
    "environment"
  )

  object JavaApi extends ScalaModule(
    "javaapi"
  )

  object JavaImpl extends ScalaModule(
    "javaimpl",
    JavaApi,
    mvn.`org.reactivestreams:reactive-streams:jar:1.0.0`
  )

  object Common extends ScalaModule(
    "common",
    mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
    mvn.`io.monix:monix_2.11:jar:2.0.4`,
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
  ) {
    object R1 extends Release(
      mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
      mvn.`io.monix:monix_2.11:jar:2.0.4`,
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
    )
  }

  object Pickling extends ScalaModule(
    "pickling",
    Common,
    mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
  ) {
  }

  object Logging extends ScalaModule(
    "logging",
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
  ) {
    object R1 extends Release(
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
    )
  }

  object Jms extends ScalaModule(
    "jms",
    Logging.R1,
    mvn.`javax.jms:jms-api:jar:1.1-rev-1`,
    mvn.`io.monix:monix_2.11:jar:2.0.2`
  ) {
    object R1 extends Release(
      Logging.R1,
      mvn.`javax.jms:jms-api:jar:1.1-rev-1`,
      mvn.`io.monix:monix_2.11:jar:2.0.2`
    )
  }

  object Packaging extends ScalaModule(
    "packaging",
    MavenModulesBuilder,
    Pickling,
    mvn.`org.scala-lang.modules:scala-xml_2.11:jar:1.0.6`,
    mvn.`org.apache.maven.shared:maven-invoker:2.2`,
    mvn.`org.scala-sbt:io_2.11:jar:1.0.0-M3`
  )

  object Macros extends ScalaModule(
    "macros",
    mvn.`org.scala-lang:scala-reflect:jar:2.11.8`
  ) {
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

object JarTreeModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "jartree")

  object Api extends ScalaModule(
    "api",
    Toolbox6Modules.JavaApi
  ) {
    object R1 extends Release(
      mvn.`org.glassfish:javax.json:jar:1.0.4`
    )
  }

  object Util extends ScalaModule(
    "util",
    Api,
    mvn.`commons-io:commons-io:jar:2.5`,
    mvn.`commons-codec:commons-codec:jar:1.10`,
    mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`,
    Toolbox6Modules.JavaImpl
  ) {
    lazy val Latest = R1
    object R1 extends Release(
      Api.R1,
      mvn.`commons-io:commons-io:jar:2.5`,
      mvn.`commons-codec:commons-codec:jar:1.10`,
      mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`
    )
  }

  object Impl extends ScalaModule(
    "impl",
    Api,
    Util,
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`,
    Toolbox6Modules.Logging,
    mvn.`io.monix:monix-eval_2.11:jar:2.0.4`,
    Wiring,
    mvn.`me.chrons:boopickle_2.11:jar:1.2.4`,
    Toolbox6Modules.Common,
    mvn.`commons-io:commons-io:jar:2.2`,
    Toolbox6Modules.JavaImpl,
    Toolbox6Modules.Pickling
  ) {
    object R1 extends Release(
      Api.R1,
      Util.R1,
      mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
    )
  }

  object ServletApi extends ScalaModule(
    "servletapi",
    Api,
    mvn.`javax.servlet:servlet-api:jar:2.5`
  ) {
    object R1 extends Release(
      Api.R1,
      mvn.`javax.servlet:servlet-api:jar:2.5`
    )
  }

  object Servlet extends ScalaModule(
    "servlet",
    Impl,
    ServletApi,
    Toolbox6Modules.Logging,
    ManagementApi,
    ManagementUtils,
    Wiring,
    mvn.`io.monix:monix-execution_2.11:jar:2.0.2`,
    mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`
  ) {
    object R1 extends Release(
      Impl.R1,
      ServletApi.R1,
      Toolbox6Modules.Logging.R1,
      ManagementApi.R1,
      ManagementUtils.R1,
      Wiring.R1,
      mvn.`io.monix:monix-execution_2.11:jar:2.0.2`,
      mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`
    )
  }

  object Webapp extends ScalaModule(
    "webapp",
    Servlet,
    mvn.`ch.qos.logback:logback-classic:jar:1.1.7`
  ) {
    object R1 extends Release(
      Servlet.R1,
      mvn.`ch.qos.logback:logback-classic:jar:1.1.7`
    )
  }

  object Wiring extends ScalaModule(
    "wiring",
    Api,
    ServletApi,
    Util,
    mvn.`io.monix:monix_2.11:jar:2.0.4`,
    mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
    Logging,
    mvn.`me.chrons:boopickle_2.11:jar:1.2.4`
  ) {
    object R1 extends Release(
      Api.R1,
      ServletApi.R1,
      Util.R1,
      mvn.`io.monix:monix-execution_2.11:jar:2.0.2`,
      mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`
    )
  }


  object ManagementApi extends ScalaModule(
    "managementapi"
  ) {
    object R1 extends Release(
    )
  }

  object ManagementUtils extends ScalaModule(
    "managementutils",
    ManagementApi,
    Common,
    Wiring,
    ServletApi
  ) {
    object R1 extends Release(
      ManagementApi.R1,
      Common.R1
    )
  }

  object Client extends ScalaModule(
    "client",
    ManagementUtils,
    mvn.`com.oracle:wlfullclient:jar:10.3.6.0`,
    Toolbox6Modules,
    Packaging
  )

  object Akka extends ScalaModule(
    "akka",
    ServletApi,
    Toolbox6Modules.Common,
    AkkaModules.Http,
    Toolbox6Modules.Logging
  ) {
    object R1 extends Release(
      ServletApi.R1,
      AkkaModules.Http.R1
    )
  }

  object Testing extends ScalaModule(
    "testing",
    Api,
    ServletApi,
    Packaging
  )

  object Packaging extends ScalaModule(
    "packaging",
    Toolbox6Modules,
    Toolbox6Modules.Packaging,
//    Webapp,
    ServletApi,
    Util,
    Servlet,
    mvn.`org.scala-lang.modules:scala-xml_2.11:jar:1.0.6`,
    mvn.`org.scala-sbt:io_2.11:jar:1.0.0-M3`,
    mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi-maven:jar:2.2.2`,
    mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-spi-maven:jar:2.2.2`,
    mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven:jar:2.2.2`,
    mvn.`com.oracle:wlfullclient:jar:10.3.6.0`
  )

}


object MavenModulesBuilder extends MavenCentralModule(
  "maven-modules",
  "maven-modules-builder",
  "1.0.0-SNAPSHOT"
)
