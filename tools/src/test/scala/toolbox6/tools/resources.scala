package toolbox6.tools

import java.io.FileInputStream


//object TFTest {
//  def main(args: Array[String]): Unit = {
//    for {
//      f <- TF(new FileInputStream("../toolbox6/pom.xml"))
//    } {
//      println(f)
//    }
//
//
//    for {
//      r1 <- TF[String](
//        () => {
//          println("open: r1")
//          "r1"
//        },
//        x => {
//          println(s"close: ${x}")
//        }
//      )
//      r2 <- TF[String](
//        () => {
//          println("open: r2")
//          "r2"
//        },
//        x => {
//          println(s"close: ${x}")
//        }
//      )
//    } {
//      println(r2)
//    }
//
//    val r = for {
//      r1 <- TF[String](
//        () => {
//          println("open: r1")
//          "r1"
//        },
//        x => {
//          println(s"close: ${x}")
//        }
//      )
//      r2 <- TF[String](
//        () => {
//          println("open: r2")
//          "r2"
//        },
//        x => {
//          println(s"close: ${x}")
//        }
//      )
//    } yield {
//      r2
//    }
//
//    println(
//      r.extract({ x =>
//        println(s"extracting: ${x}")
//        x
//      })
//    )
//
//  }
//}
