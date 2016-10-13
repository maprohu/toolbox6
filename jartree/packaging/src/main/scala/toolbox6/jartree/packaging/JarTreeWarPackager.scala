package toolbox6.jartree.packaging

import java.io.File
import javax.json.JsonObject

import maven.modules.builder.{Module, NamedModule}
import sbt.io.IO
import toolbox6.jartree.servlet.{EmbeddedJar, JarTreeServletConfig}
import toolbox6.modules.JarTreeModules
import toolbox6.packaging.{HasMavenCoordinates, MavenCoordinatesImpl, MavenHierarchy, MavenTools}

import scala.xml.XML
import toolbox6.packaging.PackagingTools.Implicits._
import JarTreePackaging.Implicits._
import toolbox6.jartree.api.JarPlugger
import toolbox6.jartree.packaging.JarTreePackaging.RunHierarchy
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.util.ClassRequestImpl
import toolbox6.jartree.webapp.WebappServlet
import upickle.Js


/**
  * Created by martonpapp on 01/10/16.
  */
object JarTreeWarPackager {

  val PackagesFromContainer = Seq(
    mvn.`javax.servlet:servlet-api:jar:2.5`,
    mvn.`javax.jms:jms-api:jar:1.1-rev-1`,
    mvn.`com.oracle:wlthint3client:jar:10.3.6.0`,
    mvn.`com.oracle:wlfullclient:jar:10.3.6.0`
  )

  val WarClassPathModules = Seq(
    JarTreeModules.Webapp.R1
  )

  val modulesInParent =
    PackagesFromContainer
      .map(p => p:MavenHierarchy)
      .flatMap(_.jars)
      .to[Set] ++
    WarClassPathModules
      .map(p => p:MavenHierarchy)
      .flatMap(_.jars)

  def filteredHierarchy(
    module: Module
  ) : MavenHierarchy = {
    MavenHierarchy
      .moduleToHierarchy(module)
      .filter(m => !modulesInParent.contains(m))
  }

  val JarsDirName = "jars"

  def run[T](
    name: String,
    version : String = "1.0.0",
    dataPath : String,
    logPath : String,
    dataDirVersion : Int = 1,
    startup: RunHierarchy
  )(
    postProcessor: File => T
  ) : T = {

    val (runRequest, paramObject, hierarchyJars) = requestAndParamAndJars(startup)

    val embeddedJars =
      hierarchyJars
        .zipWithIndex
        .map({ case (maven, idx) =>
          (maven, s"${idx}_${maven.groupId}_${maven.artifactId}_${maven.version}${maven.classifier.map(c => s"_${c}").getOrElse("")}.jar")
        })



    val pom =
      <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>

        <groupId>{name}</groupId>
        <artifactId>{name}</artifactId>
        <version>{version}</version>
        <packaging>war</packaging>
        <build>
          <finalName>{name}</finalName>
          <plugins>
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-war-plugin</artifactId>
              <version>2.6</version>
              <configuration>
                <archive>
                  <manifestEntries>
                    <WebLogic-Application-Version>{version}</WebLogic-Application-Version>
                  </manifestEntries>
                </archive>
              </configuration>
            </plugin>
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
          WarClassPathModules.map({ m =>
            m.asPomDependency
          })
          }
        </dependencies>

      </project>

    MavenTools.runMaven(
      pom,
      "install",
      preBuild = { dir =>
        val webXml =
          <web-app xmlns="http://java.sun.com/xml/ns/javaee"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
                   version="2.5">
            <servlet>
              <servlet-name>jartree</servlet-name>
              <servlet-class>{classOf[WebappServlet].getName}</servlet-class>
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

        val weblogicXML =
          <wls:weblogic-web-app
          xmlns:wls="http://xmlns.oracle.com/weblogic/weblogic-web-app"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd http://xmlns.oracle.com/weblogic/weblogic-web-app http://xmlns.oracle.com/weblogic/weblogic-web-app/1.2/weblogic-web-app.xsd">
            <wls:container-descriptor>
              <wls:prefer-web-inf-classes>false</wls:prefer-web-inf-classes>
              <wls:prefer-application-packages>
                <wls:package-name>org.slf4j</wls:package-name>
                <wls:package-name>org.slf4j.*</wls:package-name>
                <wls:package-name>org.apache.commons.*</wls:package-name>
              </wls:prefer-application-packages>
              <wls:prefer-application-resources>
                <wls:resource-name>org.slf4j.impl.*</wls:resource-name>
              </wls:prefer-application-resources>
            </wls:container-descriptor>
          </wls:weblogic-web-app>

        XML.save(
          new File(webInfDir, "weblogic.xml").getAbsolutePath,
          weblogicXML
        )

        val runtimeDir =
          new File(dir, s"target/classes")



        val configObj = Js.Obj(
          JarTreeServletConfig.ConfigAttribute ->
            upickle.default.writeJs(
              JarTreeServletConfig(
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
                startup = runRequest,
                stdout = false,
                debug = false
              )
            ),
          JarTreeServletConfig.ParamAttribute ->
            paramObject

        )

        runtimeDir.mkdirs()
        val configString =
          upickle.json.write(
            configObj,
            2
          )
        println(configString)
        IO.write(
          new File(runtimeDir, JarTreeServletConfig.ConfigFile),
          configString
        )
      }
    ){ dir =>
      postProcessor(new File(dir, s"target/${name}.war"))
    }
  }

  def requestAndParamAndJars(
    startup: RunHierarchy
  ) = {
    val hierarchy =
      startup.toMaven

    val embeddedJars =
      hierarchy.jars

    val runRequest = hierarchy.request

    val param = hierarchy.childrenJs

    (runRequest, param, embeddedJars)
  }
}
