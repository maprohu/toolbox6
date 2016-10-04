package toolbox6.jartree.framework

import toolbox6.jartree.api._
import toolbox6.jartree.servletapi.JarTreeServletContext

/**
  * Created by martonpapp on 01/10/16.
  */
class Framework extends JarRunnable[JarTreeServletContext] {
  override def run(ctx: JarContext[JarTreeServletContext], self: ClassLoaderKey): Unit = {
    println("csuf3")
  }
}
