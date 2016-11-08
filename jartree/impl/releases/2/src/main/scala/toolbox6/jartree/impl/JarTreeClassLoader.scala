package toolbox6.jartree.impl

import java.net.{URL, URLClassLoader}
import java.util

import scala.util.Try


/**
  * Created by martonpapp on 26/08/16.
  */


class ParentLastUrlClassloader(
  urls: Seq[URL],
  parent: ClassLoader
) extends URLClassLoader(urls.toArray, parent) { self =>

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

class JarTreeClassLoader(
  val url: URL,
  val deps: Seq[JarTreeClassLoader],
  parent: ClassLoader
) extends URLClassLoader(Array(url)) { self =>

  private val cls = deps.toStream

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    try {
      super.loadClass(name, resolve)
    } catch {
      case ex : ClassNotFoundException =>
        cls
          .map({ cl =>
            try {
              Some(cl.loadClassShallow(name, resolve))
            } catch {
              case ex : ClassNotFoundException =>
                None
            }
          })
          .find(_.isDefined)
          .map(_.get)
          .getOrElse({
            parent.loadClass(name)
          })
    }
  }

  def loadClassShallow(name: String, resolve: Boolean) = {
//    try {
      super.loadClass(name, resolve)
//    } catch {
//      case ex: ClassNotFoundException =>
//        parent.loadClass(name)
//    }
  }



  import scala.collection.JavaConversions._
  def getResourcesShallow(s: String): util.Enumeration[URL] = {
    super.getResources(s)
  }
  override def getResources(s: String): util.Enumeration[URL] = {
    super.getResources(s) ++
      deps.iterator.flatMap(_.getResourcesShallow(s)) ++
      parent.getResources(s)

  }

//  val publicParent = new PublicClassLoader(
//    parent
//  )

//  private val urlClassLoader = new URLClassLoader(Array(url), null) {
//    def findClassPublic(name: String): Class[_] = {
//      findClass(name)
//    }
//    override def findClass(name: String): Class[_] = {
//      try {
//        super.findClass(name)
//      } catch {
//        case ex : ClassNotFoundException =>
////          ex.printStackTrace()
//          publicParent.loadClass(name)
//      }
//    }
//
//    override def loadClass(s: String, b: Boolean): Class[_] = self.loadClass(s, b)
//  }


//  override def loadClass(name: String, resolve: Boolean): Class[_] = {
//    inner.loadClass(name, resolve)
//  }
}

//class PublicClassLoader(cl: ClassLoader) extends ClassLoader(cl) {
//  def loadClassPublic(name: String, resolve: Boolean): Class[_] = super.loadClass(name, resolve)
//  def findClassPublic(name: String): Class[_] = super.findClass(name)
//}

//class SequenceClassLoader(
//  classLoaders: Seq[JarTreeClassLoader],
//  parent: ClassLoader
//) extends ClassLoader(parent) {
//
//
//  private val cls = classLoaders
//    .toStream
//
//  override def loadClass(name: String, resolve: Boolean): Class[_] = {
//    cls
//      .map({ cl =>
//        try {
//          Some(cl.lookForClass(name))
//        } catch {
//          case ex : ClassNotFoundException =>
//            println(cl.url)
//            ex.printStackTrace()
//            None
//        }
//      })
//      .find(_.isDefined)
//      .map(_.get)
//      .getOrElse({
//        super.loadClass(name, resolve)
//      })
//  }
//}
