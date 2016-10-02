package toolbox6.jartree.servlet

import java.io.{File, InputStream, PrintWriter}
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import monix.execution.cancelables.{AssignableCancelable, CompositeCancelable}
import org.apache.commons.io.{FileUtils, IOUtils}
import toolbox6.jartree.api._
import toolbox6.jartree.impl.{JarCache, JarTree}
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

  @volatile var processor : Processor = new Processor {
    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
  }

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
      override def setProcessor(p: Processor): Unit = {
        processor = p
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
      logger.info("creating new data directory: {}", dataPath)
      Try(FileUtils.deleteDirectory(dir))
      dir.mkdirs()

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
  }


  def destroy(): Unit = {
    quietly {
      stopper.cancel()
    }
  }

  def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    processor.service(req, resp)
  }
}

object JarTreeServletConfig {

  val jconfig = Try {
    upickle.default.read[JarTreeServletConfig](
      Source.fromInputStream(
        getClass.getClassLoader.getResourceAsStream(
          JarTreeServletConfig.ClassPathResource
        )
      ).mkString
    )
  }.toOption

  val ClassPathResource = "/jartreeservlet.conf"
  val VersionFile = "jartreeservlet.version"
  val StartupFile = "jartreeservlet.startup.json"

}

case class EmbeddedJar(
  classpathResource: String,
  key: ManagedJarKeyImpl

)

case class JarTreeServletConfig(
  name: String,
  dataPath: String,
  logPath: String,
  version : Int = 1,
  embeddedJars: Seq[EmbeddedJar],
  startup : RunRequestImpl,
  stdout: Boolean = false,
  debug: Boolean = false
)
