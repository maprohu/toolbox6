package toolbox6.packaging

import jartree.util.MavenJarKeyImpl
import maven.modules.builder.{Module, NamedModule}

/**
  * Created by martonpapp on 01/10/16.
  */
object PackagingTools {


}

object HasMavenCoordinates {

  implicit def key2coords(module: MavenJarKeyImpl) : HasMavenCoordinates = {
    new HasMavenCoordinates {
      override def groupId: String = module.groupId
      override def artifactId: String = module.artifactId
      override def version: String = module.version
    }
  }

  implicit def module2coords(module: NamedModule) : HasMavenCoordinates = {
    new HasMavenCoordinates {
      override def groupId: String = module.groupId
      override def artifactId: String = module.artifactId
      override def version: String = module.version
    }
  }

}

trait HasMavenCoordinates {
  def groupId: String
  def artifactId : String
  def version: String

  def asPomCoordinates = {
    <groupId>{groupId}</groupId>
    <artifactId>{artifactId}</artifactId>
    <version>{version}</version>
  }

  def asPomDependency = {
    <dependency>
      {asPomCoordinates}
    </dependency>
  }
}



