package toolbox6.jartree.packaging

import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.jar.JarFile

import maven.modules.builder.{Module, NamedModule}
import monix.execution.atomic.Atomic
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.jboss.shrinkwrap.resolver.api.maven.Maven
import toolbox6.jartree.api.JarKey
import toolbox6.jartree.impl.JarCache
import toolbox6.jartree.util.{CaseClassLoaderKey, CaseJarKey, ClassRequestImpl, JsonTools}
import toolbox6.packaging.{HasMavenCoordinates, MavenCoordinatesImpl, MavenHierarchy}
import upickle.Js

import scala.collection.immutable.{IndexedSeq, Map, Seq}

/**
  * Created by martonpapp on 02/10/16.
  */
object JarTreePackaging {

  object Implicits {
//    implicit def moduleToCLK(module: NamedModule) : toolbox6.jartree.util.CaseClassLoaderKey = ???

  }

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



  private val ManagedIdMap = Atomic(Map[MavenCoordinatesImpl, CaseJarKey]())

  def getId(maven: MavenCoordinatesImpl) : CaseJarKey = {
    ManagedIdMap.transformAndExtract({ map =>
      map
        .get(maven)
        .map({ id => (id, map)})
        .getOrElse({
          val id = CaseJarKey(
            if (maven.isSnapshot) {
              val file = resolveFile(maven)

              val jf = new JarFile(file)
              val entry = jf.getJarEntry("META-INF/build.timestamp")

              val hash =
                Option(entry)
                  .map({ e =>
                    IOUtils.toString(jf.getInputStream(e), Charset.defaultCharset())
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

  def hierarchyToClassLoader(hierarchy: MavenHierarchy) : CaseClassLoaderKey = {
    CaseClassLoaderKey(
      getId(hierarchy.jar),
      hierarchy.dependencies.map(hierarchyToClassLoader)
    )
  }

  case class RunHierarchy(
    namedModule: Module,
    runClassName: String,
    children: Map[String, RunHierarchy] = Map()
  ) {
    def toMaven : RunMavenHierarchy = {
      val mavenHierarchy : MavenHierarchy =
        JarTreeWarPackager
          .filteredHierarchy(
            resolve(namedModule)
          )

      RunMavenHierarchy(
        mavenHierarchy,
        runClassName,
        children.mapValues(_.toMaven)
      )
    }
  }

  case class RunMavenHierarchy(
    mavenHierarchy: MavenHierarchy,
    runClassName: String,
    children: Map[String, RunMavenHierarchy] = Map()
  ) {
    def hierarchies : Seq[MavenHierarchy] = {
      mavenHierarchy +: children.values.to[Seq].flatMap(_.hierarchies)
    }

    def request = {
      ClassRequestImpl(
        JarTreePackaging.hierarchyToClassLoader(
          mavenHierarchy
        ),
        runClassName
      )
    }

    def toJsObj : Js.Obj = {
      Js.Obj(
        JsonTools.RequestAttribute ->
          ClassRequestImpl.toJsObj(
            request
          ),
        JsonTools.ParamAttribute ->
          childrenJs
      )
    }

    def childrenJs : Js.Obj = {
      Js.Obj(
        children
          .to[Seq]
          .map({
            case (key, value) =>
              key -> value.toJsObj
          }):_*
      )
    }

    def jars =
        hierarchies
        .flatMap(_.jars)
        .distinct

  }

  def resolve(module: Module) = {
    val compile =
      module.filter(!_.provided)

    val latestMap =
      compile
        .depsTransitive
        .groupBy(_.version.mavenModuleId)
        .mapValues(_.maxBy(_.version.version))

    def mapper(m: Module) : Module = {
      latestMap(m.version.mavenModuleId)
        .map(mapper)
    }

    compile
      .map(mapper)
      .flatten
  }

}
