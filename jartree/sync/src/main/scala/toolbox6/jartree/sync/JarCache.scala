package toolbox6.jartree.sync

import java.io.File
import java.net.URLEncoder

import com.typesafe.scalalogging.StrictLogging
import toolbox6.jartree.common.ParentLastUrlClassloader

/**
  * Created by maprohu on 21-11-2016.
  */
class JarCache(
  dir: File
) extends StrictLogging {
  dir.mkdirs()
  logger.info(s"starting jar cache in ${dir}")

  def get(
    key: JarKey
  ) : File = {
    new File(
      dir,
      URLEncoder.encode(
        s"${key.groupId}:${key.artifactId}:${key.version}:${key.classifier.getOrElse("")}:${key.hash.getOrElse("")}.jar",
        "UTF-8"
      )
    )
  }

  def classLoader(
    jars: Vector[JarKey],
    parent: ClassLoader
  ) = {
    if (jars.isEmpty) {
      parent
    } else {
      new ParentLastUrlClassloader(
        jars.map(j => get(j).toURI.toURL),
        parent
      )
    }
  }

  def loadInstance[T](
    config: ClassLoaderConfig[T],
    parent: ClassLoader
  ) = {
    val cl = classLoader(
      config.jars,
      parent
    )

    val clazz = cl.loadClass(config.className)

    clazz.newInstance().asInstanceOf[T]
  }

}

