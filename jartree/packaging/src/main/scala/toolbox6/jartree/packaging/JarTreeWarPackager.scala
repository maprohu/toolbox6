package toolbox6.jartree.packaging

import java.io.File

import jartree.util.CaseClassLoaderKey
import maven.modules.builder.NamedModule
import sbt.io.IO
import toolbox6.jartree.servlet.{EmbeddedJar, JarTreeServletConfig}
import toolbox6.jartree.util.RunRequestImpl
import toolbox6.modules.JarTreeModules
import toolbox6.packaging.HasMavenCoordinates._
import toolbox6.packaging.{HasMavenCoordinates, MavenTools}

import scala.xml.XML


/**
  * Created by martonpapp on 01/10/16.
  */
object JarTreeWarPackager {



  def run(
    name: String,
    version : String = "1.0.0",
    dataPath : String,
    logPath : String,
    dataDirVersion : Int = 1,
    startup: NamedModule,
    startupClass: String
  ) = {

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
                      <artifactItem>
                        <groupId>org.apache.felix</groupId>
                        <artifactId>org.apache.felix.framework</artifactId>
                        <version>5.4.0</version>
                        <overWrite>true</overWrite>
                        <destFileName>felix.jar</destFileName>
                      </artifactItem>
                      {
//                      bundles.map({ bundle =>
//                        <artifactItem>
//                          {Module.asPomCoordinates(bundle)}
//                          <overWrite>true</overWrite>
//                          <destFileName>{bundle.artifactId}.jar</destFileName>
//                        </artifactItem>
//                      })
                      }
                    </artifactItems>
                    <outputDirectory>target/classes/jartreeservlet</outputDirectory>
                  </configuration>
                </execution>
              </executions>
            </plugin>
          </plugins>
        </build>
        <dependencyManagement>
          <dependencies>
            <dependency>
              <groupId>javax.servlet</groupId>
              <artifactId>servlet-api</artifactId>
              <version>2.5</version>
              <scope>provided</scope>
            </dependency>
          </dependencies>
        </dependencyManagement>
        <dependencies>
          {
          JarTreeModules.Servlet.asPomDependency
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
              <servlet-class>{classOf[toolbox6.jartree.servlet.JarTreeServlet]}</servlet-class>
              <load-on-startup>1</load-on-startup>
            </servlet>
            <servlet-mapping>
              <servlet-name>jartree</servlet-name>
              <url-pattern>/*</url-pattern>
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

        val runRequest = RunRequestImpl(
          CaseClassLoaderKey(startup),
          startupClass

        )

        runtimeDir.mkdirs()
        import upickle.default._
        IO.write(
          new File(runtimeDir, JarTreeServletConfig.StartupFile),
          write(
            JarTreeServletConfig(
              name = name,
              dataPath = dataPath,
              logPath = logPath,
              version = dataDirVersion,
              embeddedJars = embeddedJars
            )
          )
        )
      }
    ){ dir =>
    }
  }
}
