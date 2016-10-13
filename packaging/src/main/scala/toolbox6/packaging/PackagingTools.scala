package toolbox6.packaging

import java.net.URLEncoder

import maven.modules.builder.{ModuleVersion, Module, NamedModule}
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

case class MavenCoordinatesImpl(
  groupId: String,
  artifactId: String,
  version: String,
  override val classifier : Option[String] = None
) extends HasMavenCoordinates {

}

object MavenCoordinatesImpl extends HasMavenCoordinatesImplicits {

//  implicit def fromCJR(cjk: CaseJarKey) : MavenCoordinatesImpl = {
//    cjk match {
//      case m :MavenJarKeyImpl
//    }
//
//  }

}

case class MavenHierarchy(
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


object HasMavenCoordinates extends HasMavenCoordinatesImplicits{


}

trait HasMavenCoordinatesImplicits {
  implicit def toImpl(coords: HasMavenCoordinates) : MavenCoordinatesImpl = {
    MavenCoordinatesImpl(
      coords.groupId,
      coords.artifactId,
      coords.version
    )
  }


  implicit def mavenModuleVersion2coords(module: ModuleVersion) : MavenCoordinatesImpl = {
    MavenCoordinatesImpl(
      module.mavenModuleId.groupId,
      module.mavenModuleId.artifactId,
      module.version.toString
    )
  }

  implicit def module2coords(module: Module) : MavenCoordinatesImpl = {
    module.version match {
      case m : ModuleVersion =>
        m
      case _ => ???
    }
  }

  implicit def namedModule2coords(module: NamedModule) : MavenCoordinatesImpl = {
    MavenCoordinatesImpl(
      module.groupId,
      module.artifactId,
      module.version
    )
  }

  implicit def key2maven(module: MavenCentralModule) : MavenCoordinatesImpl = {
    MavenCoordinatesImpl(
      module.groupId,
      module.artifactId,
      module.version
    )
  }

}

trait HasMavenCoordinates {
  def groupId: String
  def artifactId : String
  def version: String
  def classifier : Option[String] = None

  def asPomCoordinates : NodeBuffer = {
    val gav =
      <groupId>{groupId}</groupId>
      <artifactId>{artifactId}</artifactId>
      <version>{version}</version>

    gav &+
      classifier.map(c => <classifier>{c}</classifier>).getOrElse()
  }

  def asPomDependency = {
    <dependency>
      {asPomCoordinates}
    </dependency>
  }

  def toCanonical = {
    s"${groupId}:${artifactId}:jar:${version}"
  }

  def toFileName = {
    s"${URLEncoder.encode(toCanonical, "UTF-8")}.jar"
  }

  def isSnapshot = version.endsWith("SNAPSHOT")
}



