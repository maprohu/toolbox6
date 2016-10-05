package toolbox6.jartree.servlet

import java.io.{ByteArrayInputStream, File, InputStream, PrintWriter}
import java.rmi.RemoteException
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import monix.execution.atomic.Atomic
import monix.execution.cancelables.{AssignableCancelable, CompositeCancelable}
import org.apache.commons.io.{FileUtils, IOUtils}
import toolbox6.common.ManagementTools
import toolbox6.jartree.api._
import toolbox6.jartree.impl.{JarCache, JarTree}
import toolbox6.jartree.managementapi.{JarTreeManagement, LogListener, Registration}
import toolbox6.jartree.managementutils.JarTreeManagementUtils
import toolbox6.jartree.servlet.JarTreeServletConfig.Plugger
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.util.{CaseJarKey, ClassRequestImpl}
import toolbox6.jartree.wiring.{Plugged, SimpleJarSocket}
import toolbox6.logging.LogTools

import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Created by martonpapp on 01/10/16.
  */
class JarTreeServlet extends HttpServlet with LazyLogging with LogTools {
  val impl = new JarTreeServletImpl

  override def init(config: ServletConfig): Unit = {
    super.init(config)
    JarTreeServletConfig.jconfig.map({ jconfig =>
      impl.init(
        config,
        jconfig.name,
        jconfig.dataPath,
        jconfig.version,
        jconfig
          .embeddedJars
          .map({ jar =>
            (
              jar.key,
              () => classOf[JarTreeServlet].getClassLoader.getResourceAsStream(jar.classpathResource)
            )
          }),
        jconfig.startup
      )
    })
  }

  override def destroy(): Unit = {
    impl.destroy()
    super.destroy()
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    impl.service(req, resp)
  }
}

class JarTreeServletImpl extends LazyLogging with LogTools {

  val VoidProcessor : Processor = new Processor {
    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
    override def close(): Unit = ()
  }

  val processor : Atomic[Processor] = Atomic(VoidProcessor)

  implicit val codec = Codec.UTF8

  val stopper = CompositeCancelable()

  class StartupIO(startupFile: File) {

    def writeStartup(startup: ClassRequestImpl[Plugger]) : Unit = synchronized {
      new PrintWriter(startupFile) {
        write(
          upickle.default.write(
            startup,
            2
          )
        )
        close
      }
    }

    def read = synchronized {
      upickle.default.read[ClassRequestImpl[Plugger]](
        Source.fromFile(startupFile).mkString
      )
    }

  }

  def init(
    config: ServletConfig,
    name: String,
    dataPath: String,
    version : Int = 1,
    embeddedJars: Seq[(CaseJarKey, () => InputStream)],
    initialStartup : ClassRequestImpl[Plugger]
  ): Unit = {
    logger.info("starting {}", name)

    val context : JarTreeServletContext = new JarTreeServletContext {
      override def servletConfig(): ServletConfig = config
    }



    val dir = new File(dataPath)
    val versionFile = new File(dir, JarTreeServletConfig.VersionFile)
    val cacheDir = new File(dir, "cache")

    val startupIO = new StartupIO(
      new File(dir, JarTreeServletConfig.StartupFile)
    )

    val cache = if (
      Try(Source.fromFile(versionFile).mkString.toInt)
        .toOption
        .forall(_ < version)
    ) {
      Try(FileUtils.deleteDirectory(dir))
      dir.mkdirs()

      logger.info("creating new data directory: {}", dataPath)

      val cache = JarCache(cacheDir)

      embeddedJars
        .foreach({
          case (key, jar) =>
            quietly {
              cache.putStream(
                key,
                jar
              )
            }
        })

      quietly {
        startupIO.writeStartup(initialStartup)
      }

      new PrintWriter(versionFile) {
        write(version.toString)
        close
      }

      cache
    } else {
      logger.info("using existing data directory: {}", dataPath)
      JarCache(cacheDir)
    }

//    logger.info("starting {}", name)


    val jarTree = new JarTree(
      getClass.getClassLoader,
      cache
    )

    val processorSocket = new SimpleJarSocket[Processor, JarTreeServletContext](
      VoidProcessor,
      jarTree,
      context,
      new ClosableJarCleaner(VoidProcessor)
    )

    val startup = startupIO.read

    processorSocket.plug(
      startup
    )

    stopper += Cancelable({
      () => processorSocket.clear()
    })

    stopper += setupManagement(
      name,
      cache,
      jarTree,
      context
    )
  }

  def setupManagement(
    name: String,
    cache: JarCache,
    jarTree: JarTree,
    ctx: JarTreeServletContext
  ) = {
    ManagementTools.bind(
      JarTreeManagementUtils.bindingName(
        name
      ),
      new JarTreeManagement {
        @throws(classOf[RemoteException])
        override def sayHello(): String = "hello"

        @throws(classOf[RemoteException])
        override def registerLogListener(listener: LogListener): Registration = {
          listener.entry("one")
          listener.entry("two")
          listener.entry("three")

          new Registration {
            @throws(classOf[RemoteException])
            override def unregister(): Unit = ()
          }
        }

        @throws(classOf[RemoteException])
        override def verifyCache(ids: Array[String]): Array[Int] = {
          ids
            .zipWithIndex
            .collect({
              case (id, idx) if !cache.contains(id) => idx
            })
        }

        @throws(classOf[RemoteException])
        override def putCache(id: String, data: Array[Byte]): Unit = {
          cache.putStream(
            CaseJarKey(id),
            () => new ByteArrayInputStream(data)
          )
        }

        @throws(classOf[RemoteException])
        override def executeByteArray(classLoaderJson: String, input: Array[Byte]): Array[Byte] = {
          val request =
            ClassRequestImpl.fromString[JarRunnableByteArray[JarTreeServletContext]](classLoaderJson)

          jarTree
            .resolve(request)
            .run(input, ctx, request)
        }
      }
    )

  }


  def destroy(): Unit = {
    quietly {
      stopper.cancel()
    }
  }

  def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    processor.get.service(req, resp)
  }
}

object JarTreeServletConfig {

  type Plugger = JarPlugger[Processor, JarTreeServletContext]

  val ConfigFile = "jartreeservlet.conf.json"
  val VersionFile = "jartreeservlet.version"
  val StartupFile = "jartreeservlet.startup.json"

  lazy val jconfig =
    try {
      Some {
        val is =
          JarTreeServletConfig.getClass.getClassLoader.getResourceAsStream(
            ConfigFile
          )
        upickle.default.read[JarTreeServletConfig](
          Source.fromInputStream(
            is
          ).mkString
        )
      }
    } catch {
      case ex : Throwable =>
        ex.printStackTrace()
        None
    }


}

case class EmbeddedJar(
  classpathResource: String,
  key: CaseJarKey

)

case class JarTreeServletConfig(
  name: String,
  dataPath: String,
  logPath: String,
  version : Int,
  embeddedJars: Seq[EmbeddedJar],
  startup : ClassRequestImpl[JarPlugger[Processor, JarTreeServletContext]],
  stdout: Boolean,
  debug: Boolean
)
