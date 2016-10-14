package toolbox6.modules

import maven.modules.builder.ModuleRelease

/**
  * Created by pappmar on 13/10/2016.
  */
object RunToolbox6Release {

  val Releases = Seq(
    Toolbox6Modules.Common.R1,
    Toolbox6Modules.Logging.R1,

    JarTreeModules.Api.R1,
    JarTreeModules.ServletApi.R1,
    JarTreeModules.Util.R1,
    JarTreeModules.Wiring.R1,
    JarTreeModules.ManagementApi.R1,
    JarTreeModules.ManagementUtils.R1,
    JarTreeModules.Impl.R1,
    JarTreeModules.Servlet.R1,
    JarTreeModules.Webapp.R1,

    AkkaModules.Http.R1,
    JarTreeModules.Akka.R1,
    AkkaModules.Json4s.R1,
    Toolbox6Modules.Macros.R1,
    Toolbox6Modules.Jms.R1



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

object RunToolbox6ReleaseInstall {
  def main(args: Array[String]): Unit = {
    RunToolbox6Release.Releases.foreach { r =>
      println(r.getClass.getName)

      ModuleRelease.installRelease(
        RunToolbox6.Roots,
        r
      )
    }

  }
}
