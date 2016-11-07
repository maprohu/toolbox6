package toolbox6.jartree.servlet

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import toolbox6.common.{HygienicThread, ManagementTools}
import toolbox6.jartree.api._
import toolbox6.jartree.impl.JarTreeBootstrap.{Config, Initializer}
import toolbox6.jartree.impl._
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}
import toolbox6.jartree.wiring.SimpleJarSocket
import toolbox6.logging.LogTools

import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by martonpapp on 01/10/16.
  */
abstract class JarTreeServlet extends HttpServlet with LazyLogging with LogTools { self =>

  val VoidProcessor : Processor = new Processor {
    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
    override def close(): Unit = ()
  }

  case class Running(
    service: (HttpServletRequest, HttpServletResponse) => Unit,
    stop: Cancelable
  )

  var running = Running(
    service = (_, _) => (),
    stop = Cancelable.empty
  )

  def jarTreeBootstrapConfig : JarTreeBootstrapConfig[Processor, JarTreeServletContext]

  override def init(): Unit = {
    super.init()

    val bootstrapConfig = jarTreeBootstrapConfig

    implicit val (sch, stopEC) = HygienicThread.createSchduler()


    import bootstrapConfig._

    val rt = JarTreeBootstrap
      .init[Processor, JarTreeServletContext](
      Config(
        contextProvider = { (jt, ctx) =>
          new JarTreeServletContext {
            override def servletConfig: ServletConfig = self.getServletConfig
            override def jarTreeContext: JarTreeContext = ctx
            override implicit val executionContext: ExecutionContext = sch
            override def resolve(request: JarSeq)(implicit executionContext: ExecutionContext): Future[ClassLoader] = jt.resolve(request)
          }
        },
        voidProcessor = VoidProcessor,
        name = name,
        dataPath = dataPath,
        version = version,
        initializer = { () =>
          val init = initializer()
          import init._

          Initializer(
            embeddedJars =
              embeddedJars
                .map({ jar =>
                  (
                    jar.key,
                    () => classOf[JarTreeServlet].getClassLoader.getResourceAsStream(jar.classpathResource)
                  )
                }),
            startup = Some(startup)
          )
        },
        closer = _.close()
      )
    )

    val stopManagement = setupManagement(
      name = name,
      jarTree = rt.jarTree,
      jarCache = rt.jarCache,
      processorSocket = rt.processorSocket,
      webappVersion = self.getClass.getPackage.getImplementationVersion
    )

    running = Running(
      service = (req, res) => rt.processorSocket.currentInstance.instance.service(req, res),
      stop = Cancelable({ () =>
        logger.info("stopping management")
        quietly { stopManagement.cancel() }
        logger.info("stopping runtime")
        quietly { rt.stop.cancel() }
        logger.info("stopping ec")
        quietly { stopEC() }
        logger.info("servlet stopped")
      })
    )
  }

  override def destroy(): Unit = {
    quietly { running.stop.cancel() }
    super.destroy()
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    running.service(req, resp)
  }

  def setupManagement(
    name: String,
    jarTree: JarTree,
    jarCache: JarCache,
    processorSocket: SimpleJarSocket[Processor, JarTreeServletContext],
    webappVersion: String
  )(implicit
    executionContext: ExecutionContext
  ) : Cancelable


}

object JarTreeServlet {



}

