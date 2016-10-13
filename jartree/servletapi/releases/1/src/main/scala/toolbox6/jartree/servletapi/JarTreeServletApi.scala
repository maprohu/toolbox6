package toolbox6.jartree.servletapi

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import toolbox6.jartree.api.{Closable, InstanceResolver, JarUpdatable}


trait JarTreeServletContext extends InstanceResolver {
  def servletConfig() : ServletConfig
}

trait Processor extends JarUpdatable with Closable {
  def service(req: HttpServletRequest, resp: HttpServletResponse): Unit
}
