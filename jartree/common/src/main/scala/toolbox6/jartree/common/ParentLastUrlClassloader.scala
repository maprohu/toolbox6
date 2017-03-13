package toolbox6.jartree.common

import java.net.{URL, URLClassLoader}

import scala.util.Try

class ParentLastUrlClassloader(
  urls: Seq[URL],
  parent: ClassLoader
) extends URLClassLoader(
  urls.toArray,
  parent
) { self =>

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    Option(findLoadedClass(name))
      .orElse(
        Try(findClass(name)).toOption
      )
      .map({ c =>
        if (resolve) {
          resolveClass(c)
        }
        c
      })
      .getOrElse(
        super.loadClass(name, resolve)
      )
  }
}

