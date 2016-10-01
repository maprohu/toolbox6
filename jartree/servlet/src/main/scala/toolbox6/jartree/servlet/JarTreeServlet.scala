package toolbox6.jartree.servlet

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import toolbox6.jartree.impl.JarTree
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}

import scala.io.{Codec, Source}

/**
  * Created by martonpapp on 01/10/16.
  */
class JarTreeServlet extends HttpServlet {

  @volatile var processor : Processor = new Processor {
    override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()
  }

  val context = new JarTreeServletContext {
    override def setProcessor(p: Processor): Unit = {
      processor = p
    }
  }

  override def init(config: ServletConfig): Unit = {
    val jconfig = upickle.default.read[JarTreeServletConfig](
      Source.fromInputStream(
        getClass.getClassLoader.getResourceAsStream(
          JarTreeServletConfig.ClassPathResource
        )
      )(Codec.UTF8).mkString
    )

    JarTree()

    super.init(config)
  }

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    processor.service(req, resp)
  }
}

object JarTreeServletConfig {

  val ClassPathResource = "/jartreeservlet.conf"

}

sealed case class JarTreeServletConfig(
  path: String
)
