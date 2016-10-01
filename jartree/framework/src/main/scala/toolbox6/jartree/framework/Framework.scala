package toolbox6.jartree.framework

import toolbox6.jartree.api.{JarContext, JarRunnable, JarRunning}
import toolbox6.jartree.servletapi.JarTreeServletContext

/**
  * Created by martonpapp on 01/10/16.
  */
class Framework extends JarRunnable[JarTreeServletContext] {
  override def run(ctx: JarContext[JarTreeServletContext]): JarRunning = {
    println("csuf2")

    new JarRunning {
      override def stop(): Unit = ()
    }
  }
}
