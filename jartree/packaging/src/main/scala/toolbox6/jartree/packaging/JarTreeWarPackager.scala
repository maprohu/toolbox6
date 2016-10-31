package toolbox6.jartree.packaging

import java.io.File

import maven.modules.builder.MavenTools.ProjectDef
import maven.modules.builder.{MavenCoordinatesImpl, MavenTools, Module, NamedModule}
import maven.modules.utils.Repo
import sbt.io.IO
import toolbox6.jartree.api.JarPlugger
import toolbox6.jartree.impl.{EmbeddedJar, JarTreeBootstrapConfig, Startup}
import toolbox6.modules.JarTreeModules

import scala.xml.{Elem, NodeSeq, XML}
import toolbox6.packaging.PackagingTools.Implicits._
import toolbox6.jartree.packaging.JarTreePackaging.RunHierarchy
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.wiring.PlugRequestImpl
import toolbox6.packaging.MavenHierarchy


/**
  * Created by martonpapp on 01/10/16.
  */
object JarTreeWarPackager {



  val JarsDirName = "jars"

  case class Input(
    name: String,
    version : String = "1.0.0",
    dataPath : String,
    logPath : String,
    dataDirVersion : Int = 1,
    startup: NamedModule,
    runClassName: String,
    runtime: NamedModule = JarTreeModules.Webapp
  )

  def run[T](
    servletClassName : String,
    input: Input
  )(
    postProcessor: File => T
  ) : T = {
    import input._

    val output = process(
      servletClassName,
      input
    )

    MavenTools.runMaven(
      output.pom,
      "install",
      preBuild = output.preBuild
    ){ dir =>
      postProcessor(new File(dir, s"target/${name}.war"))
    }
  }



//  case class Output(
//    pom : Elem,
//    preBuild : File => Unit
//
//  )

  val PackagesFromContainer = Seq(
    mvn.`javax.servlet:servlet-api:jar:2.5`,
    mvn.`javax.jms:jms-api:jar:1.1-rev-1`,
    mvn.`com.oracle:wlthint3client:jar:10.3.6.0`,
    mvn.`com.oracle:wlfullclient:jar:10.3.6.0`
  )

  def requestAndParamAndJars(
    startup: NamedModule,
    runClassName: String,
    runtime: NamedModule = JarTreeModules.Webapp
  ) = {
    val modulesInParent =
      PackagesFromContainer
        .map(p => p:MavenHierarchy)
        .flatMap(_.jars)
        .to[Set] ++
        (runtime:MavenHierarchy).jars

    val runHierarchy =
      RunHierarchy(
        startup,
        None,
        runClassName
      ).forTarget(modulesInParent.contains)


    val embeddedJars =
      runHierarchy.jars

    val runRequest = runHierarchy.request[JarPlugger[Processor, JarTreeServletContext]]

    (runRequest, embeddedJars)
  }

  def process(
    servletClassName : String,
    input : Input
  ) : ProjectDef = {
    import input._

    val (runRequest, hierarchyJars) =
      requestAndParamAndJars(
        startup,
        runClassName,
        runtime
      )

    val embeddedJars =
      hierarchyJars
        .zipWithIndex
        .map({ case (maven, idx) =>
          (maven, s"${idx}_${maven.groupId}_${maven.artifactId}_${maven.version}${maven.classifier.map(c => s"_${c}").getOrElse("")}.jar")
        })

    val coords =
      MavenCoordinatesImpl(
        groupId = name,
        artifactId = s"${name}-war",
        version = version
      )

    ProjectDef(
      coordinates = coords,
      pom = {
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>

          {coords.asPomCoordinates}
          <packaging>war</packaging>
          <build>
            <finalName>{name}</finalName>
            <plugins>
              {
  //              <plugin>
  //                <groupId>org.apache.maven.plugins</groupId>
  //                <artifactId>maven-war-plugin</artifactId>
  //                <version>2.6</version>
  //                <configuration>
  //                  <archive>
  //                    <manifestEntries>
  //                      <WebLogic-Application-Version>{version}</WebLogic-Application-Version>
  //                    </manifestEntries>
  //                  </archive>
  //                </configuration>
  //              </plugin>
              }
              <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>2.10</version>
                <executions>
                  <execution>
                    <id>copy</id>
                    <phase>generate-resources</phase>
                    <goals>
                      <goal>copy</goal>
                    </goals>
                    <configuration>
                      <artifactItems>
                        {
                        embeddedJars.map({ case (jar, fn) =>
                          <artifactItem>
                            {jar.asPomCoordinates}
                            <overWrite>true</overWrite>
                            <destFileName>{fn}</destFileName>
                          </artifactItem>
                        })
                        }
                      </artifactItems>
                      <outputDirectory>target/classes/{JarsDirName}</outputDirectory>
                    </configuration>
                  </execution>
                </executions>
              </plugin>
            </plugins>
          </build>
          <dependencyManagement>
            <dependencies>
              {
              PackagesFromContainer.map({ p =>
                <dependency>
                  {p.asPomCoordinates}
                  <scope>provided</scope>
                </dependency>
              })
              }
            </dependencies>
          </dependencyManagement>
          <dependencies>
            {
            input.runtime.pomDependency
            }
          </dependencies>
          <repositories>
            {
            input.startup.repos.map { r =>
              <repository>
                <id>{r.id}</id>
                <url>{r.url}</url>
              </repository>
            }
            }
          </repositories>
        </project>
      },
      preBuild = { dir =>
        val webXml =
          <web-app xmlns="http://java.sun.com/xml/ns/javaee"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
                   version="2.5">
            <servlet>
              <servlet-name>jartree</servlet-name>
              <servlet-class>{servletClassName}</servlet-class>
              <load-on-startup>1</load-on-startup>
            </servlet>
            <servlet-mapping>
              <servlet-name>jartree</servlet-name>
              <url-pattern>{"/*"}</url-pattern>
            </servlet-mapping>
          </web-app>

        val webInfDir =
          new File(dir, "src/main/webapp/WEB-INF")
        webInfDir.mkdirs()

        XML.save(
          new File(webInfDir, "web.xml").getAbsolutePath,
          webXml
        )

        val runtimeDir =
          new File(dir, s"target/classes")

        import toolbox6.pickling.PicklingTools._

        IO.write(
          new File(runtimeDir, JarTreeBootstrapConfig.ConfigFile),
          pickle(
            JarTreeBootstrapConfig(
              name = name,
              dataPath = dataPath,
              logPath = logPath,
              version = dataDirVersion,
              embeddedJars = embeddedJars.map({ case (coords, fn) =>
                EmbeddedJar(
                  s"${JarsDirName}/${fn}",
                  JarTreePackaging.getId(coords)
                )
              }),
              startup =
                Startup(
                  PlugRequestImpl(
                    runRequest
                  )
                ),
              stdout = false,
              debug = false
            )
          )
        )
      }

    )
  }


}
