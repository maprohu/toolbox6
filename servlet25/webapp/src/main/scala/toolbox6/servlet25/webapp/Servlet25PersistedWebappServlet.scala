package toolbox6.servlet25.webapp
import java.io.File
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import jartree.impl.servlet.PersistedRunner
import jartree.impl.{JarCache, JarResolver, JarTree}
import jartree.{JarTreeRunner, RunRequest}
import sbt.io.IO
import toolbox6.servlet25.runapi.{Servlet25Context, Servlet25Runner}
import toolbox6.servlet25.singleapi.{Servlet25SingleApi, Servlet25SingleHandler}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

/**
  * Created by pappmar on 30/08/2016.
  */
class Servlet25PersistedWebappServlet(
  workDir: File,
  initializer: JarTree => Unit,
  classLoader: ClassLoader,
  initialRun: RunRequest
)(implicit
  executionContext: ExecutionContext
) extends Servlet25WebappServlet { self =>

  val runner = new PersistedRunner[Servlet25Runner](
    workDir,
    initializer,
    classLoader,
    initialRun
  )


  val context = new Servlet25Context {
    override def servlet(): HttpServlet = self

    override def jarTree(): JarTree = runner.jarTree

    override def setStartup(runRequest: RunRequest): Unit = runner.StateIO.writeState(runRequest)

    override def setHandler(handler: Option[Servlet25SingleHandler]): Unit = singleApi.set(handler)
  }

  override def init(config: ServletConfig): Unit = {
    super.init(config)

    start()
  }

  def start() = {
    runner.start(_.run(context))
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    Option(req.getPathInfo) match {
      case Some("/_admin/reset") =>
        start()
      case _ =>
        super.service(req, resp)
    }
  }
}

