package toolbox6.jartree.impl

import java.io._
import java.nio.ByteBuffer
import java.rmi.RemoteException

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import monix.execution.cancelables.CompositeCancelable
import org.apache.commons.io.{FileUtils, IOUtils}
import toolbox6.common.ByteBufferTools
import toolbox6.jartree.api._
import toolbox6.jartree.impl.JarTreeBootstrap.Config
import toolbox6.jartree.util.{CaseJarKey, ClassRequestImpl, JsonTools, RunTools}
import toolbox6.jartree.wiring.{PlugRequestImpl, SimpleJarSocket}
import toolbox6.logging.LogTools
import upickle.Js

import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Created by martonpapp on 15/10/16.
  */
object JarTreeBootstrap extends LazyLogging with LogTools {
  case class Config[Processor <: JarUpdatable, Context](
    contextProvider: JarTree => Context,
    voidProcessor : Processor,
    name: String,
    dataPath: String,
    version : Int = 1,
    embeddedJars: Seq[(CaseJarKey, () => InputStream)],
    initialStartup: PlugRequestImpl[Processor, Context],
    runtimeVersion: String
  )

  case class Runtime[Processor <: JarUpdatable, Context <: InstanceResolver](
    stop: Cancelable,
    cache: JarCache,
    jarTree: JarTree,
    ctx: Context,
    processorSocket: SimpleJarSocket[Processor, Context]
  )
  def init[Processor <: JarUpdatable with Closable, Context <: InstanceResolver](
    config : Config[Processor, Context]
  ) : Runtime[Processor, Context] = {
    import config._
    logger.info("starting {}", name)

    type Plugger = JarPlugger[Processor, Context]
    type Startup = PlugRequestImpl[Processor, Context]

    var processor : () => Processor = null

    implicit val codec = Codec.UTF8

    val stopper = CompositeCancelable()

    val dir = new File(dataPath)
    val startupFile = new File(dir, JarTreeBootstrapConfig.StartupFile)
    val versionFile = new File(dir, JarTreeBootstrapConfig.VersionFile)
    val cacheDir = new File(dir, "cache")

    def writeStartup(
      startup: Startup
    ) : Unit = this.synchronized {
      import boopickle.Default._
      ByteBufferTools
        .writeFile(
          Pickle
            .intoByteBuffers(
              startup
            )
            .toArray,
          startupFile
        )
    }

    def readStartup : Startup = this.synchronized {
      import boopickle.Default._
      Unpickle[Startup].fromBytes(
        ByteBufferTools
          .readFile(
            startupFile
          )
      )
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
        writeStartup(
          initialStartup
        )
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
      JarTreeBootstrap.getClass.getClassLoader,
      cache
    )

    val context = contextProvider(jarTree)

    val processorSocket = new SimpleJarSocket[Processor, Context](
      voidProcessor,
      context,
      new ClosableJarCleaner(voidProcessor)
    )
    processor = () => processorSocket.get()

    import rx.Ctx.Owner.Unsafe._
    val obs = processorSocket.dynamic.foreach({ p =>
      p.request.foreach({ r =>
        writeStartup(r)
      })
    })
    stopper += Cancelable(() => obs.kill())


    val startupRequest = readStartup

    processorSocket.plug(
      startupRequest
    )

    stopper += Cancelable({
      () => processorSocket.clear()
    })

    Runtime(
      stopper,
      cache,
      jarTree,
      context,
      processorSocket
    )
  }
}


object JarTreeBootstrapConfig {

  var verbose = true

  val ConfigFile = "jartreebootstrap.conf"
  val VersionFile = "jartreebootstrap.version"
  val StartupFile = "jartreebootstrap.startup"
  val SuppressInitErrorSystemPropertyName = s"${getClass.getName}.suppressInitError"

  val RuntimeVersionAttribute = "runtimeVersion"
  val ConfigAttribute = "config"
  val ParamAttribute = "param"

  lazy val jconfig : Option[JarTreeBootstrapConfig] =
    try {
      val is =
        JarTreeBootstrapConfig
          .getClass
          .getClassLoader
          .getResourceAsStream(
            ConfigFile
          )

      import boopickle.Default._

      Some(
        Unpickle[JarTreeBootstrapConfig]
          .fromBytes(
            ByteBuffer
              .wrap(
                IOUtils
                  .toByteArray(
                    is
                  )
              )
          )
      )
    } catch {
      case ex : Throwable =>
        if (verbose && System.getProperty(SuppressInitErrorSystemPropertyName) == null) {
          ex.printStackTrace()
        }
        None
    }


}

case class EmbeddedJar(
  classpathResource: String,
  key: CaseJarKey

)

case class JarTreeBootstrapConfig(
  name: String,
  dataPath: String,
  logPath: String,
  version : Int,
  embeddedJars: Seq[EmbeddedJar],
  startup : PlugRequestImpl[JarUpdatable, Any],
  stdout: Boolean,
  debug: Boolean
) {
  def plugger[Processor <: JarUpdatable, Context] =
    startup.asInstanceOf[PlugRequestImpl[Processor, Context]]
}