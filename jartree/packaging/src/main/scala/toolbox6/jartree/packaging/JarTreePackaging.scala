package toolbox6.jartree.packaging

import java.io.{FileInputStream, InputStream}
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
import scala.io.Source

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

  def clear() = {
    ManagedIdMap.set(Map())
  }

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

//                    val is =
//                      jf.getInputStream(e)
                    Source.fromInputStream(
                      jf.getInputStream(e),
//                      new InputStream {
//                        override def read(): Int = is.read()
//                      },
                      "UTF-8"
                    ).mkString

//                    IOUtils.toString(jf.getInputStream(e), Charset.defaultCharset())
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

  lazy val rootJars = JarTreeWarPackager.modulesInParent.map(getId)

  def getJars(clk: Option[CaseClassLoaderKey]) : Set[CaseJarKey] = {
    clk
      .map(c => getJars(c.parentOpt) ++ c.jarsSeq)
      .getOrElse(rootJars)
  }

  def hierarchyToClassLoader(
    hierarchy: MavenHierarchy,
    parent: Option[CaseClassLoaderKey]
  ) : CaseClassLoaderKey = {
    val parentJars = getJars(parent)

    val jars =
      hierarchy
        .jars
        .map(getId)
        .filterNot(parentJars.contains)
        .distinct

    CaseClassLoaderKey(
      jars,
      parent
    )
  }

  case class RunHierarchy(
    namedModule: Module,
    parent: Option[CaseClassLoaderKey] = None,
    runClassName: String,
    children: CaseClassLoaderKey => Map[String, RunHierarchy] = _ => Map()
  ) {
    def forWar : RunMavenHierarchy = {
      forTarget(
        JarTreeWarPackager.modulesInParent.contains
      )
    }

    def forTarget(
      targetContains: MavenCoordinatesImpl => Boolean
    ) : RunMavenHierarchy = {
      val mavenHierarchy : MavenHierarchy =
        MavenHierarchy
          .moduleToHierarchy(resolve(namedModule))
          .filter(m => !targetContains(m))

      RunMavenHierarchy(
        mavenHierarchy,
        parent,
        runClassName,
        cl => children(cl).mapValues(_.forTarget(targetContains))
      )
    }

  }

  case class RunMavenHierarchy(
    mavenHierarchy: MavenHierarchy,
    parent: Option[CaseClassLoaderKey],
    runClassName: String,
    children: CaseClassLoaderKey => Map[String, RunMavenHierarchy] = _ => Map()
  ) {
    def hierarchies : Seq[MavenHierarchy] = {
      mavenHierarchy +: children(classLoader).values.to[Seq].flatMap(_.hierarchies)
    }

    def classLoader : CaseClassLoaderKey = {
      JarTreePackaging.hierarchyToClassLoader(
        mavenHierarchy,
        parent
      )
    }

    def request = {
      ClassRequestImpl(
        classLoader,
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
        children(classLoader)
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


  def target(
    modules: NamedModule*
  ) = {
    modules
      .map(p => p:MavenHierarchy)
      .flatMap(_.jars)
      .toSet
      .contains _
  }


}
