package toolbox6.jartree.impl

import java.io._
import java.util.jar.{JarEntry, JarFile, JarInputStream}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.{Cancelable, Scheduler}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.{FileUtils, IOUtils}
import toolbox6.jartree.api._
import toolbox6.jartree.wiring.{Input, SimpleJarSocket}
import toolbox6.logging.LogTools
import toolbox6.pickling.PicklingTools

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.{Codec, Source}
import scala.util.Try
import scala.collection.immutable._

/**
  * Created by martonpapp on 15/10/16.
  */
object JarTreeBootstrap extends LazyLogging with LogTools {
//  type CTX = ScalaInstanceResolver

  case class Initializer[Processor, CtxApi](
    embeddedJars: Seq[(JarKey, () => InputStream)],
    startup: Option[ClassRequest[JarPlugger[Processor, CtxApi]]]
  )

  case class Config[Processor, CtxApi](
    contextProvider: (JarTree, JarTreeContext) => CtxApi,
    voidProcessor : Processor,
    name: String,
    dataPath: String,
    version : Option[String],
    initializer: () => Initializer[Processor, CtxApi],
    closer: Processor => Unit,
    logFile: Option[File] = None,
    storageDir: Option[File] = None
  )

  case class Runtime[Processor, CtxApi](
    stop: Cancelable,
    jarTree: JarTree,
    jarCache: JarCache,
    ctx: CtxApi,
    processorSocket: SimpleJarSocket[Processor, CtxApi]
  )
  def init[Processor, CtxApi](
    config : Config[Processor, CtxApi]
  )(implicit
    scheduler: Scheduler
  ) : Runtime[Processor, CtxApi] = {
    import config._
    logger.info("starting {}", name)

    type Plugger = JarPlugger[Processor, CtxApi]
    type Startup = ClassRequest[Plugger]
    type StartupRequest = Option[Startup]

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
      import PicklingTools._
      PicklingTools.toFile(
        startup,
        startupFile
      )
    }

    def readStartup : StartupRequest = this.synchronized {
      import PicklingTools._
      PicklingTools
        .fromFile[StartupRequest](startupFile)
    }

    val cache = if (
      version
        .forall({ v =>
          Try(Source.fromFile(versionFile).mkString)
            .toOption
            .forall(_ != v)
        })
    ) {
      logger.info("deleting old data directory: {}", dataPath)
      Try(FileUtils.deleteDirectory(dir))
      if (dir.exists()) {
        logger.warn("could not delete old directory: {}", dataPath)
      }
      dir.mkdirs()

      logger.info("creating new data directory: {}", dataPath)

      val cache = JarCache(cacheDir)

      val initial = initializer()
      import initial._

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
          startup
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

    val classLoader =
      JarTreeBootstrap.getClass.getClassLoader

    val jarTree = new JarTree(
      classLoader,
      cache
    )

    val jarTreeContext = JarTreeContext(
      name = name,
      log = logFile,
      storage = storageDir,
      cache = cache
    )

    val context : CtxApi = contextProvider(jarTree, jarTreeContext)

    val processorSocket = new SimpleJarSocket[Processor, CtxApi](
      Input(
        voidProcessor,
        context,
        new JarPlugger[Processor, CtxApi] {
          override def pull(params: PullParams[Processor, CtxApi]): Future[JarPlugResponse[Processor]] = {
            import params._
            Future.successful(
              JarPlugResponse[Processor](
                voidProcessor,
                () => closer(previous)
              )
            )
          }
        },
        jarTree,
        classLoader,
        preProcessor = { propt =>
          propt.foreach(o => writeStartup(Some(o)))
          Future.successful()
        }

      )
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

    startupRequest.foreach { sr =>
      processorSocket.plug(
        sr
      )
    }


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
      cache,
      context,
      processorSocket
    )
  }
}


object JarTreeBootstrapConfig {
  val VersionFile = "jartreebootstrap.version"
  val StartupFile = "jartreebootstrap.startup"

  def getSnapshotHashId(
    jar: () => InputStream
  ) : String = {

    val jf = new JarInputStream(jar())

    try {
      Iterator
        .continually(jf.getNextJarEntry)
        .takeWhile(_ != null)
        .find(_.getName == "META-INF/build.timestamp")
        .map({ e =>
          Source
            .fromInputStream(
              jf,
              "UTF-8"
            )
            .mkString
        })
        .getOrElse({
          Base64.encodeBase64String(
            JarCache.calculateHash(
              jar()
            )
          )
        })
    } finally {
      jf.close()
    }
  }

//  var verbose = true

//  val ConfigFile = "jartreebootstrap.conf"
//  val SuppressInitErrorSystemPropertyName = s"${getClass.getName}.suppressInitError"

//  val RuntimeVersionAttribute = "runtimeVersion"
//  val ConfigAttribute = "config"
//  val ParamAttribute = "param"

//  lazy val jconfig : Option[JarTreeBootstrapConfig] =
//    try {
//      val is =
//        JarTreeBootstrapConfig
//          .getClass
//          .getClassLoader
//          .getResourceAsStream(
//            ConfigFile
//          )
//
//      import boopickle.Default._
//
//      Some(
//        Unpickle[JarTreeBootstrapConfig]
//          .fromBytes(
//            ByteBuffer
//              .wrap(
//                IOUtils
//                  .toByteArray(
//                    is
//                  )
//              )
//          )
//      )
//    } catch {
//      case ex : Throwable =>
//        if (verbose && System.getProperty(SuppressInitErrorSystemPropertyName) == null) {
//          ex.printStackTrace()
//        }
//        None
//    }





}

case class EmbeddedJar(
  classpathResource: String,
  key: JarKey
)

//case class Startup(
//  classLoader: Seq[JarKey],
//  className: String
//) {
//  def request[T, C] = PlugRequest[T, C](
//    ClassRequest[JarPlugger[T, C]](
//      JarSeq(classLoader),
//      className
//    )
//  )
//}
//
//object Startup {
//  import toolbox6.pickling.PicklingTools._
//  implicit val pickler : PicklingTools.Pickler[Startup] = generatePickler[Startup]
//
//  def apply[T, C](
//    request: PlugRequest[T, C]
//  ) : Startup = Startup(
//    request.request.jars.jars,
//    request.request.className
//  )
//}

case class JarTreeInitializer[T, C](
  embeddedJars: Seq[EmbeddedJar],
  startup : ClassRequest[JarPlugger[T, C]]
)

case class JarTreeBootstrapConfig[T, C](
  name: String,
  dataPath: String,
  logPath: String,
  storagePath: Option[String],
  version : Option[String],
  initializer: () => JarTreeInitializer[T, C]
)