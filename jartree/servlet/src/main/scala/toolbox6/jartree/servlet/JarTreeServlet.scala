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
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.util.{CaseJarKey, ManagedJarKeyImpl, RunRequestImpl}
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
              () => getClass.getClassLoader.getResourceAsStream(jar.classpathResource)
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

  val processor : Atomic[Processor] = Atomic(new Processor {
    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
    override def close(): Unit = ()
  })

  implicit val codec = Codec.UTF8

  val stopper = CompositeCancelable()

  def init(
    config: ServletConfig,
    name: String,
    dataPath: String,
    version : Int = 1,
    embeddedJars: Seq[(ManagedJarKeyImpl, () => InputStream)],
    initialStartup : RunRequestImpl
  ): Unit = {
    logger.info("starting {}", name)

    val context = new JarTreeServletContext {
      override def setProcessor(newProcessor: Processor): Unit = {
        val oldProcessor = processor.transformAndExtract({ previous =>
          (previous, newProcessor)
        })

        quietly {
          oldProcessor.close()
        }
      }
      override def servletConfig(): ServletConfig = config
    }

    val dir = new File(dataPath)
    val versionFile = new File(dir, JarTreeServletConfig.VersionFile)
    val startupFile = new File(dir, JarTreeServletConfig.StartupFile)
    val cacheDir = new File(dir, "cache")

    def writeStartup(startup: RunRequestImpl) : Unit = synchronized {
      new PrintWriter(startupFile) {
        write(
          upickle.default.write(
            startup
          )
        )
        close
      }

    }

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
        writeStartup(initialStartup)
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

    val startup =
      upickle.default.read[RunRequestImpl](
        Source.fromFile(startupFile).mkString
      )

    val jarContext = new JarContext[JarTreeServletContext] {
      override def deploy(jar: DeployableJar): Unit = {
        cache.putStream(
          CaseJarKey(jar.key),
          () => jar.data
        )
      }
      override def setStartup(startup: RunRequest): Unit = {
        writeStartup(
          RunRequestImpl(startup)
        )
      }
      override def extension(): JarTreeServletContext = context
    }
    val runnable = jarTree.resolve[JarRunnable[JarTreeServletContext]](startup)

    val running = runnable.run(jarContext)

    stopper += Cancelable(() => running.stop())

    stopper += setupManagement(
      name,
      cache,
      jarTree,
      jarContext
    )
  }

  def setupManagement(
    name: String,
    cache: JarCache,
    jarTree: JarTree,
    ctx: JarContext[JarTreeServletContext]
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
            ManagedJarKeyImpl(id),
            () => new ByteArrayInputStream(data)
          )
        }

        @throws(classOf[RemoteException])
        override def executeByteArray(classLoaderJson: String, input: Array[Byte]): Array[Byte] = {
          jarTree
            .resolve[JarRunnableByteArray[JarTreeServletContext]](
              RunRequestImpl.fromString(classLoaderJson)
            )
            .run(input, ctx)
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
  key: ManagedJarKeyImpl

)

case class JarTreeServletConfig(
  name: String,
  dataPath: String,
  logPath: String,
  version : Int,
  embeddedJars: Seq[EmbeddedJar],
  startup : RunRequestImpl,
  stdout: Boolean,
  debug: Boolean
)
