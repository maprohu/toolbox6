package toolbox6.jartree.testing

import toolbox6.jartree.packaging.JarTreeWarPackager
import toolbox6.modules.JarTreeModules

import scala.io.StdIn

/**
  * Created by martonpapp on 02/10/16.
  */
object RunWarPackager {

  def main(args: Array[String]): Unit = {
    JarTreeWarPackager.run(
      "testing",
      "1.0.0",
      "/wl_domains/testing/data",
      "/wl_domains/testing/log",
      1,
      JarTreeModules.Framework,
      "toolbox6.jartree.framework.Framework"
    )

    StdIn.readLine("enter...")
  }

}
