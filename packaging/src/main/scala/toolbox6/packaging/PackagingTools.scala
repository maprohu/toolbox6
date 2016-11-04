package toolbox6.packaging

import java.net.URLEncoder

import mvnmod.builder.{HasMavenCoordinates, MavenCentralModule, MavenCoordinatesImpl}

import scala.collection.immutable._



final case class MavenHierarchy(
  jar: MavenCoordinatesImpl,
  dependencies: Seq[MavenHierarchy]
) {
  def jars: Seq[MavenCoordinatesImpl] =
    jar +: dependencies.flatMap(_.jars)

  def filter(fn: MavenCoordinatesImpl => Boolean) : MavenHierarchy = {
    MavenHierarchy(
      jar,
      dependencies
        .filter(h => fn(h.jar))
        .map(_.filter(fn))
    )
  }

}

object MavenHierarchy {
//  implicit def namedModuleToHierarchy(module: NamedModule) : MavenHierarchy = {
//    MavenHierarchy(
//      HasMavenCoordinates.namedModule2coords(module),
//      module.deps.to[Seq].map(moduleToHierarchy)
//    )
//  }

//  implicit def moduleToHierarchy(module: Module) : MavenHierarchy = {
//    MavenHierarchy(
//      HasMavenCoordinates.module2coords(module),
//      module
//        .deps
//        .filterNot(_.provided)
//        .map(moduleToHierarchy)
//    )
//  }

  implicit def fromCLK(clk: MavenCentralModule) : MavenHierarchy = {
    MavenHierarchy(
      MavenCoordinatesImpl.key2maven(clk),
      clk.dependencies.map(fromCLK)
    )
  }
}







