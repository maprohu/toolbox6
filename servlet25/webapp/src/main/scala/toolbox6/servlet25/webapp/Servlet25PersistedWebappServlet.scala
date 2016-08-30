package toolbox6.servlet25.webapp
import java.io.File
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import jartree.{JarCache, JarResolver, JarTree, RunRequest}
import sbt.io.IO
import toolbox6.servlet25.runapi.{Servlet25Context, Servlet25Runner}
import toolbox6.servlet25.singleapi.{Servlet25SingleApi, Servlet25SingleHandler}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by pappmar on 30/08/2016.
  */
class Servlet25PersistedWebappServlet(
  workDir: File,
  initializer: JarCache => Unit,
  classLoader: ClassLoader,
  initialRun: RunRequest
) extends Servlet25WebappServlet { self =>
  import Servlet25PersistedWebappServlet._

  private val jarCache = JarCache(
    new File(workDir, JarCacheDirName)
  )

  private val jarResolver = JarResolver(jarCache)

  private val jarTree = JarTree(classLoader, jarResolver)

  override def init(config: ServletConfig): Unit = {
    super.init(config)

    start()
  }

  val context = new Servlet25Context {
    override def servlet(): HttpServlet = self

    override def jarTree(): JarTree = self.jarTree

    override def setStartup(runRequest: RunRequest): Unit = StateIO.writeState(runRequest)

    override def setHandler(handler: Option[Servlet25SingleHandler]): Unit = singleApi.set(handler)
  }

  def start() = {
    try {
      initializer(jarCache)

      jarTree.clear()

      import scala.concurrent.ExecutionContext.Implicits.global

      val result = Await.result(
        jarTree.run[Servlet25Runner](
          StateIO.readState(),
          _.run(context)
        ),
        1.minute
      )

      result.left.foreach({ missing =>
        logger.warn("missing jars: {}", missing)
      })
    } catch {
      case ex : Throwable =>
        logger.error("error starting servlet", ex)
    }

  }

  object StateIO {

    val stateFile =
      new File(workDir, PersistedStateFileName)

    def readState() : RunRequest = synchronized {

      import upickle.default._

      if (stateFile.exists()) {
        try {
          read[RunRequest](IO.read(stateFile))
        } catch {
          case ex : Throwable =>
            logger.error("error reading persisted state", ex)
            initialRun
        }
      } else {
        initialRun
      }

    }

    def writeState(state: RunRequest) : Unit = synchronized {
      import upickle.default._

      workDir.mkdirs()

      try {
        IO.write(
          stateFile,
          write(state, indent = 4)
        )
      } catch {
        case ex : Throwable =>
          logger.error("error writing state", ex)
      }
    }

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

object Servlet25PersistedWebappServlet {

  val PersistedStateFileName = "state.dat"
  val JarCacheDirName = "jar-cache"

}
