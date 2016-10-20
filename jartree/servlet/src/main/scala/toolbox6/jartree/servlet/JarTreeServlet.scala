package toolbox6.jartree.servlet

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.rmi.RemoteException
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.LazyLogging
import toolbox6.common.ManagementTools
import toolbox6.jartree.api._
import toolbox6.jartree.impl.JarTreeBootstrap.Config
import toolbox6.jartree.impl.{JarCache, JarTree, JarTreeBootstrap, JarTreeBootstrapConfig}
import toolbox6.jartree.managementapi.JarTreeManagement
import toolbox6.jartree.managementutils.{JarTreeManagementUtils, QueryResult}
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.util._
import toolbox6.jartree.wiring.{PlugRequestImpl, SimpleJarSocket}
import toolbox6.javaapi.AsyncValue
import toolbox6.javaimpl.JavaImpl
import toolbox6.logging.LogTools

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by martonpapp on 01/10/16.
  */
class JarTreeServlet extends HttpServlet with LazyLogging with LogTools { self =>

  class ScalaJarTreeServletContext(jarTree: JarTree) extends JarTreeServletContext with ScalaInstanceResolver {
    override def servletConfig(): ServletConfig = self.getServletConfig
    override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    override def resolve[T](request: ClassRequest[T]): Future[T] = jarTree.resolve(request)
  }

  val VoidProcessor : Processor = new Processor {
    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
    override def close(): Unit = ()
    override def updateAsync(param: Array[Byte]): AsyncValue[Unit] = JavaImpl.asyncSuccess()
  }

  case class Running(
    service: (HttpServletRequest, HttpServletResponse) => Unit,
    stop: () => Unit
  )

  var running = Running(
    service = (_, _) => (),
    stop = () => ()
  )

  override def init(config: ServletConfig): Unit = {
    super.init(config)
    import monix.execution.Scheduler.Implicits.global

    JarTreeBootstrapConfig
      .jconfig
      .foreach({ bootstrapConfig =>
        import bootstrapConfig._

        val rt = JarTreeBootstrap
          .init[Processor, JarTreeServletContext, ScalaJarTreeServletContext](
            Config(
              contextProvider = jt => new ScalaJarTreeServletContext(jt),
              voidProcessor = VoidProcessor,
              name = name,
              dataPath = dataPath,
              version = version,
              embeddedJars =
                embeddedJars
                  .map({ jar =>
                    (
                      jar.key,
                      () => classOf[JarTreeServlet].getClassLoader.getResourceAsStream(jar.classpathResource)
                    )
                  }),
              initialStartup = plugger,
              closer = _.close()
            )
          )

        setupManagement(
          name = name,
          jarTree = rt.jarTree,
          processorSocket = rt.processorSocket,
          webappVersion = self.getClass.getPackage.getImplementationVersion
        )

        running = Running(
          service = (req, res) => rt.processorSocket.currentInstance.instance.service(req, res),
          stop = () => rt.stop.cancel()
        )
      })

  }

  override def destroy(): Unit = {
    running.stop()
    super.destroy()
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    running.service(req, resp)
  }

//
//  val VoidProcessor : Processor = new Processor {
//    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
//    override def close(): Unit = ()
//
//    override def update(param: JsonObject): Unit = ()
//  }
//
//  var processor : () => Processor = null
//
//  implicit val codec = Codec.UTF8
//
//  val stopper = CompositeCancelable()
//
//  class StartupIO(startupFile: File) {
//
//    def writeStartup(startup: ClassRequestImpl[Plugger], jsonObject: JsonObject) : Unit = synchronized {
//      val jsObj = Js.Obj(
//        JsonTools.RequestAttribute ->
//          ClassRequestImpl.toJsObj(startup),
//        JsonTools.ParamAttribute ->
//          JsonTools.fromJavax(jsonObject)
//      )
//
//      new PrintWriter(startupFile) {
//        write(
//          upickle.json.write(
//            jsObj,
//            2
//          )
//        )
//        close
//      }
//    }
//
//    def read : (ClassRequestImpl[Plugger], JsonObject) = synchronized {
//      val json = JsonTools.readJavax(startupFile)
//
//      val request = upickle.default.readJs[ClassRequestImpl[Any]](
//        JsonTools.fromJavax(
//          json.get(JsonTools.RequestAttribute)
//        )
//      ).asInstanceOf[ClassRequestImpl[Plugger]]
//
//      (request, json.getJsonObject(JsonTools.ParamAttribute))
//    }
//
//  }
//
//  def init(
//    config: ServletConfig,
//    name: String,
//    dataPath: String,
//    version : Int = 1,
//    embeddedJars: Seq[(CaseJarKey, () => InputStream)],
//    initialStartup : ClassRequestImpl[Plugger],
//    initialParam: JsonObject,
//    webappVersion: String
//  ): Unit = {
//    logger.info("starting {}", name)
//
//
//
//
//    val dir = new File(dataPath)
//    val versionFile = new File(dir, JarTreeServletConfig.VersionFile)
//    val cacheDir = new File(dir, "cache")
//
//    val startupIO = new StartupIO(
//      new File(dir, JarTreeServletConfig.StartupFile)
//    )
//
//    val cache = if (
//      Try(Source.fromFile(versionFile).mkString.toInt)
//        .toOption
//        .forall(_ < version)
//    ) {
//      Try(FileUtils.deleteDirectory(dir))
//      dir.mkdirs()
//
//      logger.info("creating new data directory: {}", dataPath)
//
//      val cache = JarCache(cacheDir)
//
//      embeddedJars
//        .foreach({
//          case (key, jar) =>
//            quietly {
//              cache.putStream(
//                key,
//                jar
//              )
//            }
//        })
//
//      quietly {
//        startupIO.writeStartup(initialStartup, initialParam)
//      }
//
//      new PrintWriter(versionFile) {
//        write(version.toString)
//        close
//      }
//
//      cache
//    } else {
//      logger.info("using existing data directory: {}", dataPath)
//      JarCache(cacheDir)
//    }
//
//    val jarTree = new JarTree(
//      getClass.getClassLoader,
//      cache
//    )
//
//    val context : JarTreeServletContext = new JarTreeServletContext {
//      override def servletConfig(): ServletConfig = config
//      override def resolve[T](request: ClassRequest[T]): T = jarTree.resolve(request)
//    }
//
//    val processorSocket = new SimpleJarSocket[Processor, JarTreeServletContext](
//      VoidProcessor,
//      context,
//      new ClosableJarCleaner(VoidProcessor)
//    )
//    processor = () => processorSocket.get()
//
//    import rx.Ctx.Owner.Unsafe._
//    val obs = processorSocket.dynamic.foreach({ p =>
//      p.request.foreach({
//        case (req, param) =>
//          startupIO.writeStartup(req, param)
//      })
//    })
//    stopper += Cancelable(() => obs.kill())
//
//
//    val (startupRequest, startupParam) = startupIO.read
//
//    processorSocket.plug(
//      startupRequest,
//      startupParam
//    )
//
//    stopper += Cancelable({
//      () => processorSocket.clear()
//    })
//
//    stopper += setupManagement(
//      name,
//      cache,
//      jarTree,
//      context,
//      processorSocket,
//      webappVersion
//    )
//  }
//
  def setupManagement(
    name: String,
    jarTree: JarTree,
    processorSocket: SimpleJarSocket[Processor, JarTreeServletContext, ScalaJarTreeServletContext],
    webappVersion: String
  )(implicit
    executionContext: ExecutionContext
  ) = {
    ManagementTools.bind(
      JarTreeManagementUtils.bindingName(
        name
      ),
      new JarTreeManagement {
        @throws(classOf[RemoteException])
        override def verifyCache(ids: Array[String]): Array[Int] = {
          ids
            .zipWithIndex
            .collect({
              case (id, idx) if !jarTree.cache.contains(id) => idx
            })
        }

        @throws(classOf[RemoteException])
        override def putCache(id: String, data: Array[Byte]): Unit = {
          jarTree.cache.putStream(
            CaseJarKey(id),
            () => new ByteArrayInputStream(data)
          )
        }

        import boopickle.Default._

        @throws(classOf[RemoteException])
        override def plug(request: Array[Byte]): Array[Byte] = {
          RunTools.runBytes {
            val req =
              Unpickle[PlugRequestImpl[Processor, JarTreeServletContext]].fromBytes(
                ByteBuffer.wrap(request)
              )

            Await.result(
              processorSocket.plug(
                req
              ),
              Duration.Inf
            )

            "plugged"
          }
        }

        @throws(classOf[RemoteException])
        override def query(): Array[Byte] = {
          RunTools.runByteArray {
            val bb = Pickle.intoBytes(
              QueryResult(
                request = processorSocket.query(),
                webappVersion = webappVersion
              )
            )
            val ba = Array.ofDim[Byte](bb.remaining())
            bb.get(ba)
            ba
          }
        }
      }
    )

  }
//
//
//  def destroy(): Unit = {
//    quietly {
//      stopper.cancel()
//    }
//  }
//
//  def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
//    processor().service(req, resp)
//  }
//}
//
//object JarTreeServletConfig {
//
//  var verbose = true
//
//  type Plugger = JarPlugger[Processor, JarTreeServletContext]
//
//  val ConfigFile = "jartreeservlet.conf.json"
//  val VersionFile = "jartreeservlet.version"
//  val StartupFile = "jartreeservlet.startup.json"
//  val SuppressInitErrorSystemPropertyName = s"${getClass.getName}.suppressInitError"
//
//  val WebappVersionAttributes = "webappVersion"
//  val ConfigAttribute = "config"
//  val ParamAttribute = "param"
//
//  lazy val jconfig : Option[(JarTreeServletConfig, JsonObject)] =
//    try {
//      val is =
//        JarTreeServletConfig.getClass.getClassLoader.getResourceAsStream(
//          ConfigFile
//        )
//
////      val str =
//
//      val reader = JsonProvider
//        .provider()
//        .createReader(new InputStreamReader(is))
//
//      val obj = reader.readObject()
//
//      Some(
//        (
//          upickle.default.readJs[JarTreeServletConfig](
//            JsonTools.fromJavax(
//              obj.getJsonObject(ConfigAttribute)
//            )
//          ),
//          obj.getJsonObject(ParamAttribute)
//        )
//      )
//    } catch {
//      case ex : Throwable =>
//        if (verbose && System.getProperty(SuppressInitErrorSystemPropertyName) == null) {
//          ex.printStackTrace()
//        }
//        None
//    }


}

//case class EmbeddedJar(
//  classpathResource: String,
//  key: CaseJarKey
//
//)
//
//case class JarTreeServletConfig(
//  name: String,
//  dataPath: String,
//  logPath: String,
//  version : Int,
//  embeddedJars: Seq[EmbeddedJar],
//  startup : ClassRequestImpl[Any],
//  stdout: Boolean,
//  debug: Boolean
//) {
//  def plugger =
//    startup.asInstanceOf[ClassRequestImpl[JarPlugger[Processor, JarTreeServletContext]]]
//}
