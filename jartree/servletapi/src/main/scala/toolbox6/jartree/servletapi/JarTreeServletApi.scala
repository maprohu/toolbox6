package toolbox6.jartree.servletapi

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait JarTreeServletContext {
  def servletConfig() : ServletConfig
  def setProcessor(processor: Processor) : Unit
}

trait Processor {
  def service(req: HttpServletRequest, resp: HttpServletResponse): Unit
  def close() : Unit
}
