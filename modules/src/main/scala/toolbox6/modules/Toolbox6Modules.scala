package toolbox6.modules

import maven.modules.builder._
import maven.modules.utils.MavenCentralModule
import toolbox6.modules.Toolbox6Modules.Common


/**
  * Created by martonpapp on 29/08/16.
  */
object Toolbox6Modules extends MavenCentralModule(
  "toolbox6-modules",
  "toolbox6-modules",
  "1.0.0-SNAPSHOT"
) {

  implicit val Root = RootModuleContainer("toolbox6")

  object Common extends ScalaModule(
    "common",
    "1.0.0-SNAPSHOT",
    mvn.`com.lihaoyi:scalarx_2.11:jar:0.3.1`,
    mvn.`io.monix:monix_2.11:jar:2.0.2`
  )

  object Logging extends ScalaModule(
    "logging",
    "1.0.0-SNAPSHOT",
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
  )

  object Packaging extends ScalaModule(
    "packaging",
    "1.0.0-SNAPSHOT",
    MavenModulesBuilder,
    mvn.`org.scala-lang.modules:scala-xml_2.11:jar:1.0.6`,
    mvn.`org.apache.maven.shared:maven-invoker:2.2`,
    mvn.`org.scala-sbt:io_2.11:jar:1.0.0-M3`
  )

}

object JarTreeModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "jartree")

  object Api extends NamedModule(
    Container,
    "api",
    "1.0.0-SNAPSHOT",
    (mvn.`org.scala-lang:scala-library:jar:2.11.8`:Module).copy(provided = true)
  )

  object Util extends ScalaModule(
    "util",
    "1.0.0-SNAPSHOT",
    Api,
    mvn.`commons-io:commons-io:jar:2.5`,
    mvn.`commons-codec:commons-codec:jar:1.10`,
    mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`
//    mvn.`org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-api-maven:jar:2.2.2`
  )

  object Impl extends ScalaModule(
    "impl",
    "1.0.0-SNAPSHOT",
    Api,
    Util,
//    mvn.`org.eclipse.aether:aether-util:jar:1.1.0`,
    mvn.`com.typesafe.scala-logging:scala-logging_2.11:jar:3.4.0`
  )

  object ServletApi extends ScalaModule(
    "servletapi",
    "1.0.0-SNAPSHOT",
    mvn.`javax.servlet:servlet-api:jar:2.5`
  )

  object Servlet extends ScalaModule(
    "servlet",
    "1.0.0-SNAPSHOT",
    Impl,
    ServletApi,
    Toolbox6Modules.Logging,
    ManagementApi,
    ManagementUtils,
    mvn.`io.monix:monix-execution_2.11:jar:2.0.2`,
    mvn.`com.lihaoyi:upickle_2.11:jar:0.4.2`,
    mvn.`ch.qos.logback:logback-classic:jar:1.1.7`
  )

  object Framework extends ScalaModule(
    "framework",
    "1.0.0-SNAPSHOT",
    Api,
    ServletApi
  )

  object Wiring extends ScalaModule(
    "wiring",
    "1.0.0-SNAPSHOT",
    Api,
    ServletApi,
    Util
  )


  object ManagementApi extends JavaModule(
    "managementapi",
    "1.0.0-SNAPSHOT",
    Module.provided(
      mvn.`org.scala-lang:scala-library:jar:2.11.8`
    )
  )

  object ManagementUtils extends ScalaModule(
    "managementutils",
    "1.0.0-SNAPSHOT",
    ManagementApi,
    Common
  )

  object Client extends ScalaModule(
    "client",
    "1.0.0-SNAPSHOT",
    ManagementUtils,
    mvn.`com.oracle:wlfullclient:jar:10.3.6.0`,
    Toolbox6Modules,
    Packaging,
    Framework
  )


  object Testing extends ScalaModule(
    "testing",
    "1.0.0-SNAPSHOT",
    Api,
    ServletApi,
    Packaging
  )

  object Packaging extends ScalaModule(
    "packaging",
    "1.0.0-SNAPSHOT",
    Toolbox6Modules,
    Toolbox6Modules.Packaging,
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
