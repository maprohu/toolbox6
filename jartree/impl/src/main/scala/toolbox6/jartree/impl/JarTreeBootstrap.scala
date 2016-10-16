package toolbox6.jartree.impl

import java.io._
import java.rmi.RemoteException
import javax.json.JsonObject
import javax.json.spi.JsonProvider

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import monix.execution.cancelables.CompositeCancelable
import org.apache.commons.io.FileUtils
import toolbox6.jartree.api._
import toolbox6.jartree.impl.JarTreeBootstrap.Config
import toolbox6.jartree.util.{CaseJarKey, ClassRequestImpl, JsonTools, RunTools}
import toolbox6.jartree.wiring.SimpleJarSocket
import toolbox6.logging.LogTools
import upickle.Js

import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Created by martonpapp on 15/10/16.
  */
object JarTreeBootstrap {
  case class Config[Processor <: JarUpdatable, Context](
    contextProvider: JarTree => Context,
    voidProcessor : Processor,
    managementSetup : JarTreeManagement => Cancelable,
    name: String,
    dataPath: String,
    version : Int = 1,
    embeddedJars: Seq[(CaseJarKey, () => InputStream)],
    initialStartup : ClassRequestImpl[JarPlugger[Processor, Context]],
    initialParam: JsonObject,
    runtimeVersion: String
  )
  def init[Processor <: JarUpdatable with Closable, Context <: InstanceResolver](
    config : Config[Processor, Context]
  ) : Cancelable = {
    new JarTreeBootstrap(
      config
    ).init()
  }
}
class JarTreeBootstrap[Processor <: JarUpdatable with Closable, Context <: InstanceResolver](
  config: Config[Processor, Context]
) extends LazyLogging with LogTools {
  import config._

  type Plugger = JarPlugger[Processor, Context]

  var processor : () => Processor = null

  implicit val codec = Codec.UTF8

  val stopper = CompositeCancelable()

  class StartupIO(startupFile: File) {

    def writeStartup(startup: ClassRequestImpl[Plugger], jsonObject: JsonObject) : Unit = synchronized {
      val jsObj = Js.Obj(
        JsonTools.RequestAttribute ->
          ClassRequestImpl.toJsObj(startup),
        JsonTools.ParamAttribute ->
          JsonTools.fromJavax(jsonObject)
      )

      new PrintWriter(startupFile) {
        write(
          upickle.json.write(
            jsObj,
            2
          )
        )
        close
      }
    }

    def read : (ClassRequestImpl[Plugger], JsonObject) = synchronized {
      val json = JsonTools.readJavax(startupFile)

      val request = upickle.default.readJs[ClassRequestImpl[Any]](
        JsonTools.fromJavax(
          json.get(JsonTools.RequestAttribute)
        )
      ).asInstanceOf[ClassRequestImpl[Plugger]]

      (request, json.getJsonObject(JsonTools.ParamAttribute))
    }

  }

  def init(): Cancelable = {
    logger.info("starting {}", name)

    val dir = new File(dataPath)
    val versionFile = new File(dir, JarTreeBootstrapConfig.VersionFile)
    val cacheDir = new File(dir, "cache")

    val startupIO = new StartupIO(
      new File(dir, JarTreeBootstrapConfig.StartupFile)
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
        startupIO.writeStartup(initialStartup, initialParam)
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

    val context = contextProvider(jarTree)

    val processorSocket = new SimpleJarSocket[Processor, Context](
      voidProcessor,
      context,
      new ClosableJarCleaner(voidProcessor)
    )
    processor = () => processorSocket.get()

    import rx.Ctx.Owner.Unsafe._
    val obs = processorSocket.dynamic.foreach({ p =>
      p.request.foreach({
        case (req, param) =>
          startupIO.writeStartup(req, param)
      })
    })
    stopper += Cancelable(() => obs.kill())


    val (startupRequest, startupParam) = startupIO.read

    processorSocket.plug(
      startupRequest,
      startupParam
    )

    stopper += Cancelable({
      () => processorSocket.clear()
    })

    stopper += setupManagement(
      cache,
      jarTree,
      context,
      processorSocket
    )

    stopper
  }

  def setupManagement(
    cache: JarCache,
    jarTree: JarTree,
    ctx: Context,
    processorSocket: SimpleJarSocket[Processor, Context]
  ) : Cancelable = {
    val management =
      new JarTreeManagement {
//        override def verifyCache(ids: Array[String]): Array[Int] = {
//          ids
//            .zipWithIndex
//            .collect({
//              case (id, idx) if !cache.contains(id) => idx
//            })
//        }

        override def putCache(id: String, data: Array[Byte]): Unit = {
          cache.putStream(
            CaseJarKey(id),
            () => new ByteArrayInputStream(data)
          )
        }

        override def plug(jarPluggerClassRequestJson: String, param: String): Array[Byte] = {
          val request =
            ClassRequestImpl.fromString[JarPlugger[Processor, Context]](jarPluggerClassRequestJson)

          val paramJson = JsonTools.readJavax(param)

          RunTools.runBytes {
            processorSocket.plug(
              request,
              paramJson
            )
            "plugged"
          }
        }

        override def query(): String = {
          val v = processorSocket.query()

          val o1 = JsonProvider
            .provider()
            .createArrayBuilder()

          val o2 =
            v
              .map({
                case (cr, o) =>
                  o1
                    .add(
                      JsonProvider
                        .provider()
                        .createObjectBuilder()
                        .add(
                          JarTreeBootstrapConfig.ConfigAttribute,
                          JsonTools.toJavax(
                            upickle.default.writeJs(cr).asInstanceOf[Js.Obj]
                          )
                        )
                        .add(
                          JarTreeBootstrapConfig.ParamAttribute,
                          o
                        )
                        .add(
                          JarTreeBootstrapConfig.RuntimeVersionAttribute,
                          runtimeVersion
                        )
                    )

              })
              .getOrElse(o1)



          JsonTools.writeJavax(
            o2.build()
          )
        }

        override def verifyCache(uniqueId: String): Boolean = ???
      }

    managementSetup(management)

  }


  def destroy(): Unit = {
    quietly {
      stopper.cancel()
    }
  }


}

object JarTreeBootstrapConfig {

  var verbose = true

  val ConfigFile = "jartreebootstrap.conf.json"
  val VersionFile = "jartreebootstrap.version"
  val StartupFile = "jartreebootstrap.startup.json"
  val SuppressInitErrorSystemPropertyName = s"${getClass.getName}.suppressInitError"

  val RuntimeVersionAttribute = "runtimeVersion"
  val ConfigAttribute = "config"
  val ParamAttribute = "param"

  lazy val jconfig : Option[(JarTreeBootstrapConfig, JsonObject)] =
    try {
      val is =
        JarTreeBootstrapConfig.getClass.getClassLoader.getResourceAsStream(
          ConfigFile
        )

      val reader = JsonProvider
        .provider()
        .createReader(new InputStreamReader(is))

      val obj = reader.readObject()

      Some(
        (
          upickle.default.readJs[JarTreeBootstrapConfig](
            JsonTools.fromJavax(
              obj.getJsonObject(ConfigAttribute)
            )
          ),
          obj.getJsonObject(ParamAttribute)
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
  startup : ClassRequestImpl[Any],
  stdout: Boolean,
  debug: Boolean
) {
  def plugger[Processor <: JarUpdatable, Context] =
    startup.asInstanceOf[ClassRequestImpl[JarPlugger[Processor, Context]]]
}