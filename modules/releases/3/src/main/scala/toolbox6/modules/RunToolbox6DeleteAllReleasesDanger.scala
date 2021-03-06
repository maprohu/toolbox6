package toolbox6.modules

import sbt.io.IO

import scala.io.StdIn
import ammonite.ops._
import mvnmod.builder.ModuleRelease


object RunToolbox6DeleteAllReleasesDanger {
  def main(args: Array[String]): Unit = {

    val dirs =
      RunToolbox6Release
        .Releases
        .map({ r =>
          require(!r.isSnapshot)
          ModuleRelease.releaseDirFor(
            RunToolbox6.Roots,
            r
          )._2
        })

    println(
      dirs
        .mkString("\n")
    )

    val s = StdIn.readLine("type 'sure':")
    require(s == "sure")

    dirs
      .foreach({ d =>
        println(s"deleting: ${d}")
        rm(d)
      })


  }
}
