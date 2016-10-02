package toolbox6.jartree.packaging

import maven.modules.builder.NamedModule
import monix.execution.atomic.Atomic
import org.apache.commons.codec.binary.Base64
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import toolbox6.jartree.api.ManagedJarKey
import toolbox6.jartree.impl.JarCache
import toolbox6.jartree.util.{CaseClassLoaderKey, ManagedJarKeyImpl}
import toolbox6.packaging.{MavenCoordinatesImpl, MavenHierarchy}

/**
  * Created by martonpapp on 02/10/16.
  */
object JarTreePackaging {

  object Implicits {
//    implicit def moduleToCLK(module: NamedModule) : toolbox6.jartree.util.CaseClassLoaderKey = ???

  }

  private val ManagedIdMap = Atomic(Map[MavenCoordinatesImpl, ManagedJarKeyImpl]())

  def getId(maven: MavenCoordinatesImpl) : ManagedJarKeyImpl = {
    ManagedIdMap.transformAndExtract({ map =>
      map
        .get(maven)
        .map({ id => (id, map)})
        .getOrElse({
          val id = ManagedJarKeyImpl(
            if (maven.isSnapshot) {
              val hash = Base64.encodeBase64String(
                JarCache.calculateHash(
                  Maven
                    .resolver()
                    .resolve(maven.toCanonical)
                    .withoutTransitivity()
                    .asSingleInputStream()
                )
              )
              s"${maven.toCanonical}:${hash}"
            } else {
              maven.toCanonical
            }
          )

          (id, map.updated(maven, id))
        })
    })

  }

  def hierarchyToClassLoader(hierarchy: MavenHierarchy) : CaseClassLoaderKey = {
    CaseClassLoaderKey(
      getId(hierarchy.jar),
      hierarchy.dependencies.map(hierarchyToClassLoader)
    )
  }



}
