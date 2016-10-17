package toolbox6.jartree.impl


import toolbox6.jartree.api._
import toolbox6.jartree.util.{CaseClassLoaderKey, CaseJarKey}

import scala.collection.mutable
import scala.ref.WeakReference

/**
  * Created by martonpapp on 27/08/16.
  */
object JarTree {

}

class JarTree(
  val parentClassLoader: ClassLoader,
  val cache: JarCache
) extends InstanceResolver {

  private val classLoaderMap = mutable.Map.empty[CaseClassLoaderKey, WeakReference[ClassLoader]]

  def clear() = synchronized {
    classLoaderMap.clear()
  }

  def get(
    key: CaseClassLoaderKey
  ) : ClassLoader = synchronized {
    classLoaderMap
      .get(key)
      .flatMap(_.get)
      .getOrElse {

        val parent =
          key
            .parentOpt
            .map({ p =>
              get(p)
            })
            .getOrElse(parentClassLoader)


        val cl = new ParentLastUrlClassloader(
          key.jarsSeq.map({ jar =>
            cache.get(jar).toURI.toURL
          }),
          parent
        )

        classLoaderMap.put(key, WeakReference(cl))

        cl
      }
  }


  def resolve[T](
    request: ClassRequest[T]
  ) : T = {
    val cl = get(CaseClassLoaderKey(request.classLoader))
    val runClass = cl.loadClass(request.className)
    val instance = runClass.newInstance().asInstanceOf[T]
    instance
  }

}



