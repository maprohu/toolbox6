package toolbox6.jartree.sync

/**
  * Created by maprohu on 05-11-2016.
  */
case class JarKey(
  groupId: String,
  artifactId: String,
  version: String,
  classifier: Option[String] = None,
  hash: Option[String] = None
) {
  def isSnapshot = version.endsWith("SNAPSHOT")
  def toCanonical = {
    s"${groupId}:${artifactId}:jar:${version}"
  }
}

case class ClassLoaderConfig[T](
  jars: Vector[JarKey],
  className: String
)