package toolbox6.jartree.packaging

import java.io.{FileInputStream, InputStream}
import java.nio.charset.Charset
import java.util.jar.JarFile

import monix.execution.atomic.Atomic
import mvnmod.builder.{HasMavenCoordinates, MavenCoordinatesImpl}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import toolbox6.jartree.api.{ClassRequest, JarKey}
import toolbox6.jartree.impl.JarCache
import upickle.Js

import scala.collection.immutable._
import scala.io.Source

/**
  * Created by martonpapp on 02/10/16.
  */
object JarTreePackaging {

  def resolveInputStream(maven: HasMavenCoordinates) = {
    Maven
      .resolver()
      .resolve(maven.toCanonical)
      .withoutTransitivity()
      .asSingleInputStream()
  }

  def resolveFile(maven: HasMavenCoordinates) = {
    Maven
      .resolver()
      .resolve(maven.toCanonical)
      .withoutTransitivity()
      .asSingleFile()
  }



  private val ManagedIdMap = Atomic(Map[MavenCoordinatesImpl, JarKey]())

  def clear() = {
    ManagedIdMap.set(Map())
  }

  def getId(mavenIn: HasMavenCoordinates) : JarKey = {
    val maven : MavenCoordinatesImpl = MavenCoordinatesImpl.toImpl(mavenIn)
    ManagedIdMap.transformAndExtract({ map =>
      map
        .get(maven)
        .map({ id => (id, map)})
        .getOrElse({
          val id = JarKey(
            if (maven.isSnapshot) {
              val file = resolveFile(maven)

              val jf = new JarFile(file)
              val entry = jf.getJarEntry("META-INF/build.timestamp")

              val hash =
                Option(entry)
                  .map({ e =>

                    Source.fromInputStream(
                      jf.getInputStream(e),
                      "UTF-8"
                    ).mkString

                  })
                  .getOrElse(
                    Base64.encodeBase64String(
                      JarCache.calculateHash(
                        new FileInputStream(file)
                      )
                    )
                  )

              jf.close()


              s"${maven.toCanonical}:${hash}"
            } else {
              maven.toCanonical
            }
          )

          (id, map.updated(maven, id))
        })
    })

  }

  def resolverJarsFile(jars: Seq[HasMavenCoordinates]) = {
    jars
      .map({ h =>

        val id = JarTreePackaging.getId(h)
        val data = JarTreePackaging.resolveFile(h)

        (id.uniqueId, data)
      })
      .toIndexedSeq
  }

  def request[T](
    classPath: Seq[HasMavenCoordinates],
    className: String
  ) = {
    ClassRequest[T](
      classPath.map(getId),
      className
    )
  }


}
