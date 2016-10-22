package toolbox6.jartree.testing

import java.io.File

import toolbox6.common.ByteBufferTools
import toolbox6.jartree.impl.Startup
import toolbox6.jartree.util.{CaseClassLoaderKey, ClassRequestImpl}
import toolbox6.jartree.wiring.PlugRequestImpl

import scala.collection.immutable._

/**
  * Created by pappmar on 20/10/2016.
  */
object RunPicklingRead {

  def main(args: Array[String]): Unit = {

    import boopickle.Default._
    val tf =
      new File("../sandbox/target/vmi.txt")
    val s3 = Unpickle[Startup]
      .fromBytes(
        ByteBufferTools.readFile(
          tf
        )
      )

    println(s3)

  }

}
