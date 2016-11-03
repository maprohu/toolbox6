package toolbox6.jartree.servletapi

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import toolbox6.jartree.api.{ClassLoaderResolver, JarTreeContext}

import scala.concurrent.ExecutionContext


trait JarTreeServletContext extends ClassLoaderResolver {
  def servletConfig: ServletConfig
  def jarTreeContext: JarTreeContext
  implicit val executionContext: ExecutionContext
}

trait Processor {
  def close() : Unit
  def service(req: HttpServletRequest, resp: HttpServletResponse): Unit
}
