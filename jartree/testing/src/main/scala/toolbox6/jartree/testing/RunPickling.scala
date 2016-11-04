package toolbox6.jartree.testing

import java.io.File

import toolbox6.common.ByteBufferTools

import scala.collection.immutable._

/**
  * Created by pappmar on 20/10/2016.
  */
object RunPickling {

//  def main(args: Array[String]): Unit = {
//    import toolbox6.pickling.PicklingTools._
//
//    val bs = pickle(
//      Startup(
//        PlugRequest(
//          ClassRequest(
//            Seq(),
//            "class"
//          )
//        )
//      )
//    )
//
//
//    val s = unpickle[Startup](bs)
//
//    println(s)
//
//
////    val file = new File("D:\\wl_domains\\frontex\\frontex-apps\\data\\ftx-core\\jartreebootstrap.startup")
////    val s2 = unpickle[Startup](IO.readBytes(file))
////    println(s2)
//
//    val tf =
//      new File("../sandbox/target/pickling.test")
//    ByteBufferTools
//      .writeFile(
//        Pickle(s)
//          .toByteBuffer,
//        tf
//      )
//
//    val s3 = Unpickle[Startup]
//      .fromBytes(
//        ByteBufferTools.readFile(
//          tf
//        )
//      )
//
//    println(s3)
//
//    val tf2 = new File("../sandbox/target/pickling.test2")
//    toFile(s3, tf2)
//    val s4 = fromFile[Startup](tf2)
//    println(s"s4: ${s4}")
//
//
//
//
//  }

}
