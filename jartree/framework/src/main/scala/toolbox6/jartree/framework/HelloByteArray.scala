package toolbox6.jartree.framework

import toolbox6.jartree.api.{JarContext, JarRunnableByteArray}
import toolbox6.jartree.servletapi.JarTreeServletContext

/**
  * Created by martonpapp on 02/10/16.
  */
class HelloByteArray extends JarRunnableByteArray[JarTreeServletContext] {
  override def run(data: Array[Byte], ctx: JarContext[JarTreeServletContext]): Array[Byte] = {
    s"Hello, ${new String(data)}!".getBytes
  }
}
