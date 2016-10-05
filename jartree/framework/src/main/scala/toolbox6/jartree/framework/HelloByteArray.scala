package toolbox6.jartree.framework

import toolbox6.jartree.api._
import toolbox6.jartree.servletapi.JarTreeServletContext

/**
  * Created by martonpapp on 02/10/16.
  */
class HelloByteArray extends JarRunnableByteArray[JarTreeServletContext] {
  override def run(data: Array[Byte], ctx: JarTreeServletContext, self: ClassRequest[JarRunnableByteArray[JarTreeServletContext]]): Array[Byte] = {
    s"Hello, ${new String(data)}!".getBytes
  }
}
