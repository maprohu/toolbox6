package toolbox6.jartree.servlet

import java.io.{File, PrintWriter}
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import monix.execution.Cancelable
import monix.execution.cancelables.{AssignableCancelable, CompositeCancelable}
import org.apache.commons.io.{FileUtils, IOUtils}
import toolbox6.jartree.api._
import toolbox6.jartree.impl.{JarCache, JarTree}
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.util.{CaseJarKey, ManagedJarKeyImpl, RunRequestImpl}

import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Created by martonpapp on 01/10/16.
  */
class JarTreeServlet extends HttpServlet {

  @volatile var processor : Processor = new Processor {
    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
  }

  implicit val codec = Codec.UTF8

  val stopper = CompositeCancelable()

  override def init(config: ServletConfig): Unit = {
    super.init(config)

    val context = new JarTreeServletContext {
      override def setProcessor(p: Processor): Unit = {
        processor = p
      }
      override def servletConfig(): ServletConfig = config
    }


    import JarTreeServletConfig.jconfig
    val dir = new File(jconfig.path)
    val versionFile = new File(dir, JarTreeServletConfig.VersionFile)
    val startupFile = new File(dir, JarTreeServletConfig.StartupFile)
    val cacheDir = new File(dir, "cache")
    val logDir = new File(dir, "log")

    def writeStarup(startup: RunRequest) : Unit = synchronized {
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
        .forall(_ < jconfig.version)
    ) {
      Try(FileUtils.deleteDirectory(dir))
      dir.mkdirs()

      val cache = JarCache(cacheDir)

      jconfig
        .embeddedJars
        .foreach({ jar =>
          cache.putStream(
            jar.key,
            () => getClass.getClassLoader.getResourceAsStream(jar.classpathResource)
          )
        })

      writeStarup(jconfig.startup)

      cache
    } else {
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
      override def setStartup(startup: RunRequest): Unit = writeStarup(startup)
      override def extension: JarTreeServletContext = context
    }
    val runnable = jarTree.resolve[JarRunnable[JarTreeServletContext]](startup)

    val running = runnable.run(jarContext)

    stopper += Cancelable(() => running.stop())

    super.init(config)
  }


  override def destroy(): Unit = {
    stopper.cancel()
    super.destroy()
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    processor.service(req, resp)
  }
}

object JarTreeServletConfig {

  val jconfig = upickle.default.read[JarTreeServletConfig](
    Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream(
        JarTreeServletConfig.ClassPathResource
      )
    ).mkString
  )

  val ClassPathResource = "/jartreeservlet.conf"
  val VersionFile = "jartreeservlet.version"
  val StartupFile = "jartreeservlet.startup"

}

sealed case class EmbeddedJar(
  classpathResource: String,
  key: ManagedJarKeyImpl

)

sealed case class JarTreeServletConfig(
  name: String,
  path: String,
  version : Int = 1,
  embeddedJars: Seq[EmbeddedJar],
  startup : RunRequestImpl,
  stdout: Boolean = false,
  debug: Boolean = false
)
