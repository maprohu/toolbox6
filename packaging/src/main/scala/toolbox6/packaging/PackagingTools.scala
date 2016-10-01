package toolbox6.packaging

/**
  * Created by martonpapp on 01/10/16.
  */
object PackagingTools {

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


