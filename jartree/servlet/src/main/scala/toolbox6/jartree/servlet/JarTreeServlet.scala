package toolbox6.jartree.servlet

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.rmi.RemoteException
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import monix.execution.cancelables.CompositeCancelable
import toolbox6.common.{HygienicThread, ManagementTools}
import toolbox6.jartree.api._
import toolbox6.jartree.impl.JarTreeBootstrap.Config
import toolbox6.jartree.impl._
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.util._
import toolbox6.jartree.wiring.{PlugRequestImpl, SimpleJarSocket}
import toolbox6.logging.LogTools

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by martonpapp on 01/10/16.
  */
abstract class JarTreeServlet extends HttpServlet with LazyLogging with LogTools { self =>

  class ScalaJarTreeServletContext(jarTree: JarTree) extends JarTreeServletContext with ScalaInstanceResolver {
    override def servletConfig(): ServletConfig = self.getServletConfig
    override implicit def executionContext: ExecutionContext = HygienicThread.Implicits.global
    override def resolve[T](request: ClassRequest[T]): Future[T] = jarTree.resolve(request)
  }

  val VoidProcessor : Processor = new Processor {
    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
    override def close(): Unit = ()
//    override def updateAsync(param: Array[Byte]): AsyncValue[Unit] = JavaImpl.asyncSuccess()
  }

  case class Running(
    service: (HttpServletRequest, HttpServletResponse) => Unit,
    stop: Cancelable
  )

  var running = Running(
    service = (_, _) => (),
    stop = Cancelable.empty
  )

  override def init(config: ServletConfig): Unit = {
    super.init(config)

    import HygienicThread.Implicits.global
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
            initialStartup = startup.request[Processor, JarTreeServletContext],
            closer = _.close()
          )
        )

        val stopManagement = setupManagement(
          name = name,
          jarTree = rt.jarTree,
          processorSocket = rt.processorSocket,
          webappVersion = self.getClass.getPackage.getImplementationVersion
        )

        running = Running(
          service = (req, res) => rt.processorSocket.currentInstance.instance.service(req, res),
          stop = CompositeCancelable(
            stopManagement,
            rt.stop
          )
        )
      })
  }

  override def destroy(): Unit = {
    quietly { running.stop.cancel() }
    quietly { HygienicThread.stopGlobal() }
    super.destroy()
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    running.service(req, resp)
  }


//  class JarTreeManagementImpl(
//    jarTree: JarTree,
//    processorSocket: SimpleJarSocket[Processor, JarTreeServletContext, ScalaJarTreeServletContext],
//    webappVersion: String
//  )(implicit
//    executionContext: ExecutionContext
//  ) extends JarTreeManagement {
//    @throws(classOf[RemoteException])
//    override def verifyCache(ids: Array[String]): Array[Int] = {
//      HygienicThread.execute {

//      }
//    }
//
//    @throws(classOf[RemoteException])
//    override def putCache(id: String, data: Array[Byte]): Unit = {
//      HygienicThread.execute {

//      }
//    }
//
//    import boopickle.Default._
//
//    @throws(classOf[RemoteException])
//    override def plug(request: Array[Byte]): Array[Byte] = {
//      HygienicThread.execute {

//      }
//    }
//
//    @throws(classOf[RemoteException])
//    override def query(): Array[Byte] = {
//      HygienicThread.execute {
//        RunTools.runByteArray {
//          val bb = Pickle.intoBytes(

//          )
//          val ba = Array.ofDim[Byte](bb.remaining())
//          bb.get(ba)
//          ba
//        }
//      }
//    }
//  }

  def setupManagement(
    name: String,
    jarTree: JarTree,
    processorSocket: SimpleJarSocket[Processor, JarTreeServletContext, ScalaJarTreeServletContext],
    webappVersion: String
  )(implicit
    executionContext: ExecutionContext
  ) : Cancelable
//  = {
//
//    val instance = new JarTreeManagementImpl(
//      jarTree,
//      processorSocket,
//      webappVersion
//    )
//
//    val ccl = Thread.currentThread().getContextClassLoader
//    val unbind = try {
//      Thread.currentThread().setContextClassLoader(classOf[HttpServlet].getClassLoader)
//
//      ManagementTools.bind(
//        existing = Seq(),
//        path = JarTreeManagementUtils.bindingNamePath(name),
//        name = JarTreeManagementUtils.MonitoringName,
//        instance
//      )
//    } finally {
//      Thread.currentThread().setContextClassLoader(ccl)
//    }
//
//    CompositeCancelable(
//      unbind,
//      Cancelable({ () =>
//        quietly {
//          val om = OIDManager.getInstance()
//          om.removeServerReference(
//            om.getServerReference(instance)
//          )
//        }
//      })
//    )
//
//
//  }


}

