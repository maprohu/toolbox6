package toolbox6.modules

import mvnmod.builder.{ModuleRelease, NamedModule}
import toolbox6.modules

/**
  * Created by pappmar on 13/10/2016.
  */
object RunToolbox6Release {

  val Releases = Seq[NamedModule#Release](

    Toolbox6Modules.Jms.R3,
    AkkaModules.Stream.R2,
    Toolbox6Modules.Modules.R6,
    JarTreeModules.Servlet.R5,
    JarTreeModules.Impl.R5,

    GisModules.Geotools.R1,
    GisModules.Util.R1,
    Toolbox6Modules.Modules.R5,
    JarTreeModules.Servlet.R4,
    JarTreeModules.Impl.R4,

    Toolbox6Modules.Jms.R2,
    Toolbox6Modules.Modules.R4,

    JarTreeModules.Akka.R1,
    AkkaModules.Http.R1,
    AkkaModules.Json4s.R1,
    Toolbox6Modules.Jms.R1,
    AkkaModules.Stream.R1,
    AkkaModules.Actor.R1,
    Toolbox6Modules.Modules.R3,
    JarTreeModules.Servlet.R3,
    JarTreeModules.Impl.R3,
    JarTreeModules.ManagementApi.R3,
    JarTreeModules.Wiring.R3,
    JarTreeModules.Util.R3,
    Toolbox6Modules.Pickling.R3,
    Toolbox6Modules.Common.R3,
    Toolbox6Modules.Logging.R2,

    Toolbox6Modules.Modules.R2,
    JarTreeModules.Servlet.R2,
    JarTreeModules.ManagementApi.R2,
    JarTreeModules.Impl.R2,
    JarTreeModules.Wiring.R2,
    JarTreeModules.ServletApi.R2,
    JarTreeModules.Util.R2,
    JarTreeModules.Api.R2,
    Toolbox6Modules.Pickling.R2,
    Toolbox6Modules.Common.R2,

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
  )

  def main(args: Array[String]): Unit = {

    Releases
      .reverse
      .foreach { r =>
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
      .take(5)
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
