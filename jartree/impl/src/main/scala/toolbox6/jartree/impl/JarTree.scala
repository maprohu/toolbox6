package toolbox6.jartree.impl

import java.io.InputStream

import toolbox6.jartree.api._
import toolbox6.jartree.impl.JarTree.{ResolutionResult, ResolutionResultAsync, Unresolved}
import toolbox6.jartree.util.{CaseClassLoaderKey, CaseJarKey}

import scala.collection.immutable._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Created by martonpapp on 27/08/16.
  */
object JarTree {
  type JarHash = Seq[Byte]
  type Unresolved = Seq[JarKey]
  type ResolutionResult[T] = Either[Unresolved, T]
  type ResolutionResultAsync[T] = Future[Either[Unresolved, T]]

//  def requestToKey(request: ClassLoaderRequest) : ClassLoaderKey = {
//    ClassLoaderKey(
//      jar = request.jar,
//      parents = request.parents.map(requestToKey)
//    )
//  }

//  def toJarCacheHash(hash: JarHash) : JarCache.Hash = {
//    hash.toArray
//  }

//  def apply(
//    parentClassLoader: ClassLoader,
//    cache: JarCache
//  ): JarTree = new JarTree(parentClassLoader, cache)
//
//  def apply(
//    parentClassLoader: ClassLoader,
//    resolver: JarResolver
//  )(implicit
//    executionContext: ExecutionContext
//  ): JarTree = new JarTree(parentClassLoader, resolver)


//  def collectMavenParents(cl: CaseClassLoaderKey) : Seq[CaseClassLoaderKey] = {
//    cl.dependenciesSeq.flatMap({ p =>
//      p +: collectMavenParents(p)
//    })
//  }

//  def flatten(cl: CaseClassLoaderKey) : CaseClassLoaderKey = {
//    val sch = new org.eclipse.aether.util.version.GenericVersionScheme()
//
//    val grouped =
//      collectMavenParents(cl)
//        .map({ pcl =>
//          pcl.jar match {
//            case mvn : MavenJarKey =>
//              val key = (mvn.groupId, mvn.artifactId, mvn.classifier)
//              val version = sch.parseVersion(mvn.version)
//              key -> (version, pcl)
//            case hash : HashJarKey =>
//              val key = hash.hash
//              val version = sch.parseVersion("1.0")
//              key -> (version, pcl)
//          }
//        })
//        .groupBy({ case (key, (version, cl)) => key })
//        .values
//
//    val newDependencies =
//      grouped
//        .map({ group =>
//          group
//            .map({ case (key, (version, cl)) => (version, cl) })
//            .maxBy({ case (version, cl) => version })
//        })
//        .map({ case (version, cl ) => cl })
//        .to[Seq]
//
//    cl.copy(dependenciesSeq = newDependencies)
//
//  }

//  val threadLocal = new ThreadLocal[JarTree]()
}

class JarTree(
  val parentClassLoader: ClassLoader,
  val cache: JarCache
) extends InstanceResolver {

  val classLoaderMap = mutable.WeakHashMap.empty[CaseClassLoaderKey, ClassLoader]
//  val classLoaderMap = mutable.WeakHashMap.empty[CaseClassLoaderKey, JarTreeClassLoader]

  def clear() = synchronized {
    classLoaderMap.clear()
  }

  def get(
    key: CaseClassLoaderKey
  ) : ClassLoader = synchronized {
    classLoaderMap
      .get(key)
      .getOrElse {
        println(key.jarsSeq.head)

        val parent =
          key
            .parentOpt
            .map({ p =>
              get(p)
            })
            .getOrElse(parentClassLoader)


        val cl = new ParentLastUrlClassloader(
          key.jarsSeq.map({ jar =>
            cache.toJarFile(jar).toURI.toURL
          }),
          parent
        )

        classLoaderMap.put(key, cl)

        cl
      }
  }
//  def get(
//    key: CaseClassLoaderKey
//  ) : JarTreeClassLoader = synchronized {
//    classLoaderMap
//      .get(key)
//      .getOrElse {
//        val parents =
//          key
//            .dependenciesSeq
//            .map(get)
//
//        val cl = new JarTreeClassLoader(
//          cache.toJarFile(key.jar).toURI.toURL,
//          parents,
//          parentClassLoader
//        )
//
//        classLoaderMap.put(key, cl)
//
//        cl
//      }
//  }


  def resolve[T](
    request: ClassRequest[T]
  ) : T = {
    val cl = get(CaseClassLoaderKey(request.classLoader))
    val runClass = cl.loadClass(request.className)
    val instance = runClass.newInstance().asInstanceOf[T]
    instance
  }

}



