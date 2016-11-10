package toolbox6.jartree.packaging

import java.io.File

import mvnmod.builder.MavenTools.ProjectDef
import mvnmod.builder._
import toolbox6.jartree.impl.JarTree
import toolbox6.jartree.servlet.JarTreeServlet

import scala.xml.{Elem, NodeSeq, XML}


/**
  * Created by martonpapp on 01/10/16.
  */
object JarTreeWarPackager {

  case class JarRef(
    jar: HasMavenCoordinates,
    classLoaderPath: String
  )

  case class Input(
    name: String,
    coords: HasMavenCoordinates,
    servletClassName : String,
    jars: Seq[HasMavenCoordinates],
    runtime: Module,
    repos: Seq[Repo],
    container: Module
  )

  def run[T](
    input: Input
  )(
    postProcessor: File => T
  ) : T = {
    import input._

    val output = process(
      input
    )

    MavenTools.runMaven(
      output.pom,
      "install",
      preBuild = output.preBuild
    ){ dir =>
      postProcessor(new File(dir, s"target/${coords.artifactId}.war"))
    }
  }




  def process(
    input : Input
  ) : ProjectDef = {
    import input._


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
                        jars.map({ jar =>
                          <artifactItem>
                            {jar.asPomCoordinates}
                            <overWrite>true</overWrite>
                            <destFileName>{JarTree.jarClassLoaderFileName(jar)}</destFileName>
                          </artifactItem>
                        })
                        }
                      </artifactItems>
                      <outputDirectory>target/classes/{JarTree.JarsPathElement}</outputDirectory>
                    </configuration>
                  </execution>
                </executions>
              </plugin>
            </plugins>
          </build>
          <dependencyManagement>
            <dependencies>
              {
              container.deps.map({ (m:Module) =>
                <dependency>
                  {m.version.asPomCoordinates}
                  <scope>provided</scope>
                </dependency>
              })
              }
            </dependencies>
          </dependencyManagement>
          <dependencies>
            {
            input.runtime.version.asPomDependency
            }
          </dependencies>
          <repositories>
            {
            repos.map { r =>
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

//        val runtimeDir =
//          new File(dir, s"target/classes")
//
//        import toolbox6.pickling.PicklingTools._
//
//        IO.write(
//          new File(runtimeDir, JarTreeBootstrapConfig.ConfigFile),
//          pickle(
//            JarTreeBootstrapConfig(
//              name = name,
//              dataPath = dataPath,
//              logPath = logPath,
//              version = dataDirVersion,
//              embeddedJars = embeddedJars.map({ case (_, fn, id) =>
//                EmbeddedJar(
//                  s"${JarsDirName}/${fn}",
//                  id
//                )
//              }),
//              startup =
//                Startup(
//                  embeddedJars.map({
//                    case (_, _, id) => id
//                  }),
//                  runClassName
//                ),
//              stdout = false,
//              debug = false
//            )
//          )
//        )
      }

    )
  }


}
