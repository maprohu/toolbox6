package toolbox6.jartree.meta

import java.io.File

import mvnmod.builder.HasMavenCoordinates

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.ref.WeakReference

/**
  * Created by martonpapp on 27/08/16.
  */
object JarTree {

  val JarsPathElement = "jars"

  def jarClassLoaderFileName(
    maven: HasMavenCoordinates
  ) : String = {
    s"${JarTree.moduleClassLoaderName(maven)}.jar"
  }

  def jarClassLoaderResourcePath(
    maven: HasMavenCoordinates
  ) : Seq[String] = {
    Seq(
      JarsPathElement,
      jarClassLoaderFileName(maven)
    )
  }

  def embeddedKey(
    module: HasMavenCoordinates
  ) = {
    JarKey(
      module.toCanonical
    )
  }

  def embeddedJar(
    module: HasMavenCoordinates
  ) : EmbeddedJar = {
    val path = jarClassLoaderResourcePath(module).mkString("/")

    EmbeddedJar(
      classpathResource = path,
      key = embeddedKey(module)
    )

  }

  def moduleClassLoaderName(
    maven: HasMavenCoordinates
  ) : String = {
    s"${maven.groupId}_${maven.artifactId}_${maven.version}${maven.classifier.map(c => s"_${c}").getOrElse("")}"
  }

  def metaModule(
    module: Module
  ) = {
    MavenCoordinatesImpl(
      module.version.groupId,
      s"${module.version.artifactId}-meta",
      module.version.version
    )
  }

  def metaClassPath(
    module: Module
  ) : Seq[String] = {
    metaClassPath(module.version)
  }

  def metaClassPath(
    coords: HasMavenCoordinates
  ) : Seq[String] = {
    Seq(
      s"${moduleClassLoaderName(coords)}.jartreemeta"
    )
  }

  def readMetaData[T](
    module: Module,
    classLoader: ClassLoader
  )(implicit
    pickler: Pickler[T]
  ) : T = {
    readMetaData(
      module.version,
      classLoader
    )
  }

  def readMetaData[T](
    coords: HasMavenCoordinates,
    classLoader: ClassLoader
  )(implicit
    pickler: Pickler[T]
  ) : T = {
    PicklingTools
      .fromInputStream[T]({ () =>
      classLoader.getResourceAsStream(metaClassPath(coords).mkString("/"))
    })
  }

  def writeMetaData[T](
    module: Module,
    data: T,
    dir: File
  )(implicit
    pickler: Pickler[T]
  ) = {
    val metaFile = new File(
      dir,
      JarTree.metaClassPath(module).mkString("/")
    )
    metaFile.getParentFile.mkdirs()
    PicklingTools
      .toFile[T](
        data,
        metaFile
      )
  }

  def readMetaJarSeq(
    module: Module,
    classLoader: ClassLoader
  ) = {
    readMetaData[JarSeq](
      module,
      classLoader
    )
  }

  def writeMetaJarSeq(
    module: Module,
    target: ModulePath,
    dir: File,
    keyProvider: HasMavenCoordinates => JarKey
  ) = {
    writeMetaData[JarSeq](
      module,
      JarSeq(
        module
          .forTarget(
            target
          )
          .classPath
          .map(keyProvider)
      ),
      dir
    )
  }


}

