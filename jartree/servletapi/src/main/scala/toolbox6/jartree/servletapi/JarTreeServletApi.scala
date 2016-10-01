package toolbox6.jartree.servletapi

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

trait JarTreeServletContext {
  def setProcessor(processor: Processor) : Unit
}

trait Processor {
  def service(req: HttpServletRequest, resp: HttpServletResponse): Unit
}
