package toolbox6.modules

import mvnmod.builder.{ModuleRelease, NamedModule}
import toolbox6.modules

/**
  * Created by pappmar on 13/10/2016.
  */
object RunToolbox6Release {

  val Releases = Seq[NamedModule#Release](

    Toolbox6Modules.Modules.R1,
    Toolbox6Modules.Environment.R1,
    JarTreeModules.Servlet.R1,
    JarTreeModules.Impl.R1,
    JarTreeModules.ManagementApi.R1,
    JarTreeModules.Wiring.R1,
    JarTreeModules.Util.R1,
    Toolbox6Modules.Macros.R1,
    Toolbox6Modules.Pickling.R1,
    Toolbox6Modules.Common.R1,
    Toolbox6Modules.Logging.R1,
    Toolbox6Modules.Logback.R1,
    JarTreeModules.ServletApi.R1,
    JarTreeModules.Api.R1


//    Toolbox6Modules.Common.R1,
//    Toolbox6Modules.Logging.R1,
//
//    JarTreeModules.Api.R1,
//    JarTreeModules.ServletApi.R1,
//    JarTreeModules.Util.R1,
//    JarTreeModules.Wiring.R1,
//    JarTreeModules.ManagementApi.R1,
//    JarTreeModules.ManagementUtils.R1,
//    JarTreeModules.Impl.R1,
//    JarTreeModules.Servlet.R1,
//    JarTreeModules.Webapp.R1,
//
//    AkkaModules.Http.R1,
//    JarTreeModules.Akka.R1,
//    AkkaModules.Json4s.R1,
//    Toolbox6Modules.Macros.R1,
//    Toolbox6Modules.Jms.R1,
//
//
//    Toolbox6Modules.Environment.R1,
//    Toolbox6Modules.JavaApi.R1,
//    Toolbox6Modules.JavaImpl.R1,
//    Toolbox6Modules.Logging.R2,
//    Toolbox6Modules.Common.R2,
//    Toolbox6Modules.Pickling.R1,
//    Toolbox6Modules.Jms.R1,
//    Toolbox6Modules.Packaging.R1,
//    Toolbox6Modules.Macros.R2,
//
//    AkkaModules.Http.R2,
//
//    JarTreeModules.Api.R2,
//    JarTreeModules.Util.R2,
//    JarTreeModules.ServletApi.R2,
//    JarTreeModules.Wiring.R2,
//    JarTreeModules.Impl.R2,
//    JarTreeModules.ManagementApi.R2,
//    JarTreeModules.ManagementUtils.R2,
//    JarTreeModules.Servlet.R2,
//    JarTreeModules.Webapp.R2,
//    JarTreeModules.Packaging.R1,
//    JarTreeModules.Client.R1,
//    JarTreeModules.Akka.R2










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
    RunToolbox6Release
      .Releases
      .reverse
      .foreach { r =>
        println(r.getClass.getName)

        ModuleRelease.installRelease(
          RunToolbox6.Roots,
          r
        )
      }

  }
}
