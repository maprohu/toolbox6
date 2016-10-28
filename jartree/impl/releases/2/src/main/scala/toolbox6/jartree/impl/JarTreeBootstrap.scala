package toolbox6.jartree.impl

import java.io._
import java.nio.ByteBuffer
import java.rmi.RemoteException

import boopickle.Pickler
import com.typesafe.scalalogging.LazyLogging
import monix.execution.{Cancelable, Scheduler}
import monix.execution.cancelables.CompositeCancelable
import org.apache.commons.io.{FileUtils, IOUtils}
import toolbox6.common.ByteBufferTools
import toolbox6.jartree.api._
import toolbox6.jartree.impl.JarTreeBootstrap.Config
import toolbox6.jartree.util._
import toolbox6.jartree.wiring.{PlugRequestImpl, SimpleJarSocket}
import toolbox6.javaapi.AsyncValue
import toolbox6.javaimpl.JavaImpl
import toolbox6.logging.LogTools
import toolbox6.pickling.PicklingTools
import upickle.Js
import scala.concurrent.duration._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Created by martonpapp on 15/10/16.
  */
object JarTreeBootstrap extends LazyLogging with LogTools {
//  type CTX = ScalaInstanceResolver

  case class Config[Processor, CtxApi <: InstanceResolver, Context <: CtxApi with ScalaInstanceResolver](
    contextProvider: JarTree => Context,
    voidProcessor : Processor,
    name: String,
    dataPath: String,
    version : Int = 1,
    embeddedJars: Seq[(CaseJarKey, () => InputStream)],
    initialStartup: PlugRequestImpl[Processor, CtxApi],
    closer: Processor => Unit
  )

  case class Runtime[Processor, CtxApi <: InstanceResolver, CtxImpl <: CtxApi with ScalaInstanceResolver](
    stop: Cancelable,
    jarTree: JarTree,
    ctx: CtxImpl,
    processorSocket: SimpleJarSocket[Processor, CtxApi, CtxImpl]
  )
  def init[Processor, CtxApi <: InstanceResolver, Context <: CtxApi with ScalaInstanceResolver](
    config : Config[Processor, CtxApi, Context]
  )(implicit
    scheduler: Scheduler
  ) : Runtime[Processor, CtxApi, Context] = {
    import config._
    logger.info("starting {}", name)

    type Plugger = JarPlugger[Processor, Context]
    type StartupRequest = PlugRequestImpl[Processor, CtxApi]

    var processor : () => Processor = null

    implicit val codec = Codec.UTF8

//    val stopper = CompositeCancelable()

    val dir = new File(dataPath)
    val startupFile = new File(dir, JarTreeBootstrapConfig.StartupFile)
    val versionFile = new File(dir, JarTreeBootstrapConfig.VersionFile)
    val cacheDir = new File(dir, "cache")

    def writeStartup(
      startup: StartupRequest
    ) : Unit = this.synchronized {
      import Startup._
      PicklingTools.toFile[Startup](
        Startup(
          startup
        ),
        startupFile
      )
    }

    def readStartup : StartupRequest = this.synchronized {
      import Startup._
      PicklingTools.fromFile[Startup](startupFile)
        .request[Processor, CtxApi]
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

    val context : Context = contextProvider(jarTree)

    val processorSocket = new SimpleJarSocket[Processor, CtxApi, Context](
      voidProcessor,
      context,
      new JarPlugger[Processor, CtxApi] {
        override def pullAsync(previous: Processor, context: CtxApi): AsyncValue[JarPlugResponse[Processor]] = {
          JavaImpl.asyncSuccess(
            new JarPlugResponse[Processor] {
              override def instance(): Processor = voidProcessor
              override def andThen(): Unit = closer(previous)
            }
          )
        }
      },
      preProcessor = { propt =>
        propt.foreach(o => writeStartup(o))
        Future.successful()
      }
    )
    processor = () => processorSocket.get()


//    import rx.Ctx.Owner.Unsafe._
//    val obs = processorSocket.dynamic.foreach({ p =>
//      p.request.foreach({ r =>
//        writeStartup(r)
//      })
//    })
//    stopper += Cancelable(() => obs.kill())


    val startupRequest = readStartup

    processorSocket.plug(
      startupRequest
    )


    Runtime(
      Cancelable({
        { () =>
          logger.info("stopping jartree bootstrap")
          processorSocket.stop()
          logger.info("clearing jartree")
          jarTree.clear()
          logger.info("jartree bootstrap stopped")
        }
      }),
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

//  val RuntimeVersionAttribute = "runtimeVersion"
//  val ConfigAttribute = "config"
//  val ParamAttribute = "param"

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

case class Startup(
  classLoader: CaseClassLoaderKey,
  className: String
) {
  def request[T, C] = PlugRequestImpl[T, C](
    ClassRequestImpl[JarPlugger[T, C]](
      classLoader,
      className
    )
  )
}

object Startup {
  import toolbox6.pickling.PicklingTools._
  implicit val pickler : PicklingTools.Pickler[Startup] = generatePickler[Startup]

  def apply[T, C](
    request: PlugRequestImpl[T, C]
  ) : Startup = Startup(
    request.request.classLoader,
    request.request.className
  )
}

case class JarTreeBootstrapConfig(
  name: String,
  dataPath: String,
  logPath: String,
  version : Int,
  embeddedJars: Seq[EmbeddedJar],
  startup : Startup,
//  startup : PlugRequestImpl[Processor, Context],
  stdout: Boolean,
  debug: Boolean
) {
//  def plugger[Processor <: JarUpdatable, Context] =
//    PlugRequestImpl[Processor, Context](
//      ClassRequestImpl[JarPlugger[Processor, Context]](
//        startup.classLoader,
//        startup.className
//      ),
//      startup.param
//    )
//    startup.asInstanceOf[PlugRequestImpl[Processor, Context]]
}