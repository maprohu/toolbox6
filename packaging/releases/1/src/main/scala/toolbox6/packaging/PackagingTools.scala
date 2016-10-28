package toolbox6.packaging

import java.net.URLEncoder

import maven.modules.builder._
import maven.modules.utils.MavenCentralModule

import scala.collection.immutable._
import scala.xml.NodeBuffer

/**
  * Created by martonpapp on 01/10/16.
  */
object PackagingTools {


  object Implicits extends HasMavenCoordinatesImplicits{


  }


}



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
  implicit def namedModuleToHierarchy(module: NamedModule) : MavenHierarchy = {
    MavenHierarchy(
      HasMavenCoordinates.namedModule2coords(module),
      module.deps.to[Seq].map(moduleToHierarchy)
    )
  }

  implicit def moduleToHierarchy(module: Module) : MavenHierarchy = {
    MavenHierarchy(
      HasMavenCoordinates.module2coords(module),
      module
        .deps
        .filterNot(_.provided)
        .map(moduleToHierarchy)
    )
  }

  implicit def fromCLK(clk: MavenCentralModule) : MavenHierarchy = {
    MavenHierarchy(
      HasMavenCoordinates.key2maven(clk),
      clk.dependencies.map(fromCLK)
    )
  }
}







