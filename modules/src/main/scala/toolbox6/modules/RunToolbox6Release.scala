package toolbox6.modules

import maven.modules.builder.ModuleRelease

/**
  * Created by pappmar on 13/10/2016.
  */
object RunToolbox6Release {

  val Releases = Seq(
    JarTreeModules.Api.R1,
    JarTreeModules.ServletApi.R1
  )

  def main(args: Array[String]): Unit = {

    Releases.foreach { r =>
      println(r.getClass.getName)

      ModuleRelease.release(
        RunToolbox6.Roots,
        r
      )
    }

  }

}
