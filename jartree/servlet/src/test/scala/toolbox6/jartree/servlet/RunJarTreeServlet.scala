package toolbox6.jartree.servlet

import java.io.{File, FileInputStream}

import org.apache.commons.io.{FileUtils, IOUtils}
import toolbox6.jartree.util.{CaseClassLoaderKey, CaseJarKey, RunRequestImpl}

import scala.collection.immutable._

/**
  * Created by martonpapp on 01/10/16.
  */
object RunJarTreeServlet {

  def main(args: Array[String]): Unit = {

    val frameworkJar =
      new File("../toolbox6/jartree/framework/target/product.jar")

    val frameworkId = CaseJarKey(frameworkJar)

    val impl = new JarTreeServletImpl()

    impl.init(
      null,
      "testing",
      "../sandbox/target/jartreeservlet",
      1,
      Seq(
        (frameworkId, () => new FileInputStream(frameworkJar))
      ),
      RunRequestImpl(
        CaseClassLoaderKey(
          frameworkId,
          Seq()
        ),
        "toolbox6.jartree.framework.Framework"
      )
    )

    impl.destroy()


  }

}
