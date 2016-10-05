package toolbox6.modules

import java.io.File

import maven.modules.builder.Module.ConfiguredModule
import maven.modules.builder.{Module, ModuleContainer, NamedModule, PlacedRoot}

import scala.collection.immutable._

/**
  * Created by pappmar on 29/08/2016.
  */
object RunToolbox6 {

  val RootDir = new File("../toolbox6")

  val Roots = Seq[PlacedRoot](
    Toolbox6Modules.Root -> RootDir
  )

  val Modules = Seq[ConfiguredModule](
    Toolbox6Modules.Common,
    Toolbox6Modules.Logging,
    Toolbox6Modules.Packaging,
    JarTreeModules.Api,
    JarTreeModules.Util,
    JarTreeModules.Impl,
    JarTreeModules.ServletApi,
    JarTreeModules.Servlet,
    JarTreeModules.Framework,
    JarTreeModules.Wiring,
    JarTreeModules.ManagementApi,
    JarTreeModules.ManagementUtils,
    JarTreeModules.Client,
    JarTreeModules.Testing,
    JarTreeModules.Packaging,
    Servlet25Modules.SingleApi,
    Servlet25Modules.RunApi,
    Servlet25Modules.SampleRunner,
    Servlet25Modules.Webapp,
    VisModules.Raw

  )

  def main(args: Array[String]): Unit = {

    Module.generate(
      Roots,
      Modules
    )

  }

  def projectDir(module: ModuleContainer) : File = {
    module.path.tail.foldLeft(RootDir)(new File(_, _))
  }

  def projectDir(module: NamedModule) : File = {
    new File(projectDir(module.container), module.name)
  }


}