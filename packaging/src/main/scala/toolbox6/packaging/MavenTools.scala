package toolbox6.packaging

import java.io.File
import java.nio.file.Files

import org.apache.maven.shared.invoker.{DefaultInvocationRequest, DefaultInvoker}
import sbt.io.IO

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.xml._

/**
  * Created by martonpapp on 01/10/16.
  */
object MavenTools {

  lazy val tmpRoot : File = {
    val file = new File("../sandbox/target/mvntmproot")
    file.mkdirs()

    val dir = Files.createTempDirectory(
      file.toPath,
      "root"
    ).toFile

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        IO.delete(dir)
      }
    })

    dir
  }

  def inTempDir[T](fn: File => T) : T = {
    val dir = Files.createTempDirectory(
      tmpRoot.toPath,
      "mvn"
    )

    fn(dir.toFile)
  }

  def runMaven[T](
    pomFileString: Node,
    goal : String,
    preBuild: File => Unit = _ => ()
  )( andThen : File => T ) : T = {
    runMavens(
      pomFileString,
      Seq(goal),
      preBuild
    )(
      andThen
    )
  }

  def runMavens[T](
    pomFileString: Node,
    goals : Seq[String],
    preBuild: File => Unit = _ => ()
  )( andThen : File => T ) : T = {
    try {
      inTempDir { dir =>
        try {
          val pomFile = new File(dir, "pom.xml")

          XML.save(pomFile.getAbsolutePath, pomFileString)

          preBuild(dir)

          val request = new DefaultInvocationRequest
          request.setPomFile(pomFile)
          request.setGoals( goals )
          val invoker = new DefaultInvoker

          val result = invoker.execute(request)

          require(result.getExitCode == 0, "maven exited with error")

          println("starting postprocessing...")
          andThen(dir)

        } finally {
          println("leaving project dir")
        }

      }
    } finally {
      println("project dir left")
    }
  }

  def runMavenProject(dir: File, goals : Seq[String]) = {
    val pomFile = new File(dir, "pom.xml")

    val request = new DefaultInvocationRequest
    request.setPomFile(pomFile)
    request.setGoals( goals )
    val invoker = new DefaultInvoker

    val result = invoker.execute(request)

    require(result.getExitCode == 0)
  }

//  def pom(content: Node) : Elem = {
//    pom(content:NodeBuffer)
//  }
//
  def pom(content: NodeSeq) : Elem = {
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
      <modelVersion>4.0.0</modelVersion>

      <groupId>toolbox6</groupId>
      <artifactId>maven-tools-temp</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      {content}
    </project>

  }


}
