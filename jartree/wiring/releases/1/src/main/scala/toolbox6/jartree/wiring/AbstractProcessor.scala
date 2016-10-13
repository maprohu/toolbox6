package toolbox6.jartree.wiring

import javax.json.JsonValue
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import toolbox6.jartree.api._
import toolbox6.jartree.servletapi.{JarTreeServletContext, Processor}

/**
  * Created by martonpapp on 05/10/16.
  */
abstract class AbstractProcessor
  extends ClosableJarPlugger[Processor, JarTreeServletContext]
  with Processor {

  override def close(): Unit = ()

}
