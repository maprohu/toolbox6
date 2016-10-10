package toolbox6.jartree.servlet

import java.io._
import java.rmi.RemoteException
import javax.json.spi.JsonProvider
import javax.json.{Json, JsonObject, JsonValue}
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
import toolbox6.jartree.util.{CaseJarKey, ClassRequestImpl, JsonTools, RunTools}
import toolbox6.jartree.wiring.{Plugged, SimpleJarSocket}
import toolbox6.logging.LogTools
import upickle.Js

import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Created by martonpapp on 01/10/16.
  */
class JarTreeServlet extends HttpServlet with LazyLogging with LogTools {
  val impl = new JarTreeServletImpl

  override def init(config: ServletConfig): Unit = {
    super.init(config)
    JarTreeServletConfig.jconfig.map({ case (jconfig, param) =>
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
        jconfig.plugger,
        param
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

    override def update(param: JsonObject): Unit = ()
  }

//  val processor : Atomic[Processor] = Atomic(VoidProcessor)
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

  def init(
    config: ServletConfig,
    name: String,
    dataPath: String,
    version : Int = 1,
    embeddedJars: Seq[(CaseJarKey, () => InputStream)],
    initialStartup : ClassRequestImpl[Plugger],
    initialParam: JsonObject
  ): Unit = {
    logger.info("starting {}", name)




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

//    logger.info("starting {}", name)


    val jarTree = new JarTree(
      getClass.getClassLoader,
      cache
    )

    val context : JarTreeServletContext = new JarTreeServletContext {
      override def servletConfig(): ServletConfig = config
      override def resolve[T](request: ClassRequest[T]): T = jarTree.resolve(request)
    }

    val processorSocket = new SimpleJarSocket[Processor, JarTreeServletContext](
      VoidProcessor,
      context,
      new ClosableJarCleaner(VoidProcessor)
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
      name,
      cache,
      jarTree,
      context,
      processorSocket
    )
  }

  def setupManagement(
    name: String,
    cache: JarCache,
    jarTree: JarTree,
    ctx: JarTreeServletContext,
    processorSocket: SimpleJarSocket[Processor, JarTreeServletContext]
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

        @throws(classOf[RemoteException])
        override def plug(jarPluggerClassRequestJson: String, param: String): Array[Byte] = {
          val request =
            ClassRequestImpl.fromString[JarPlugger[Processor, JarTreeServletContext]](jarPluggerClassRequestJson)

          val paramJson = JsonTools.readJavax(param)

          RunTools.runBytes {
            processorSocket.plug(
              request,
              paramJson
            )
            "plugged"
          }
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
    processor().service(req, resp)
  }
}

object JarTreeServletConfig {

  var verbose = true

  type Plugger = JarPlugger[Processor, JarTreeServletContext]

  val ConfigFile = "jartreeservlet.conf.json"
  val VersionFile = "jartreeservlet.version"
  val StartupFile = "jartreeservlet.startup.json"
  val SuppressInitErrorSystemPropertyName = s"${getClass.getName}.suppressInitError"

  val ConfigAttribute = "config"
  val ParamAttribute = "param"

  lazy val jconfig : Option[(JarTreeServletConfig, JsonObject)] =
    try {
      val is =
        JarTreeServletConfig.getClass.getClassLoader.getResourceAsStream(
          ConfigFile
        )

//      val str =

      val reader = JsonProvider
        .provider()
        .createReader(new InputStreamReader(is))

      val obj = reader.readObject()

      Some(
        (
          upickle.default.readJs[JarTreeServletConfig](
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

case class JarTreeServletConfig(
  name: String,
  dataPath: String,
  logPath: String,
  version : Int,
  embeddedJars: Seq[EmbeddedJar],
  startup : ClassRequestImpl[Any],
  stdout: Boolean,
  debug: Boolean
) {
  def plugger =
    startup.asInstanceOf[ClassRequestImpl[JarPlugger[Processor, JarTreeServletContext]]]
}
