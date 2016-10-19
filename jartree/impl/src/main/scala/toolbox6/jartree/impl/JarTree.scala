package toolbox6.jartree.impl


import monix.execution.atomic.Atomic
import toolbox6.jartree.api._
import toolbox6.jartree.util.{CaseClassLoaderKey, CaseJarKey}
import toolbox6.javaapi.AsyncValue
import toolbox6.javaimpl.JavaImpl

import scala.collection.mutable
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.ref.WeakReference

/**
  * Created by martonpapp on 27/08/16.
  */
object JarTree {

}


class JarTree(
  val parentClassLoader: ClassLoader,
  val cache: JarCache
)(implicit
  executionContext: ExecutionContext
) extends InstanceResolver {

  class Holder(
    val future : Future[WeakReference[ClassLoader]]
  )
  private val classLoaderMap = Atomic(Map.empty[CaseClassLoaderKey, Holder])

  def clear() = synchronized {
    classLoaderMap.set(Map.empty)
  }

  def get(
    key: CaseClassLoaderKey
  ) : Future[ClassLoader] = {

    def inner(
      exclude: Option[Holder]
    ) : Future[ClassLoader] = {
      classLoaderMap
        .transformAndExtract({ clm =>
          clm
            .get(key)
            .filterNot(exclude.contains)
            .map({ h =>
              val extract =
                h
                  .future
                  .flatMap({ wrcl =>
                    wrcl
                      .get
                      .map({ cl =>
                        Future.successful(cl)
                      })
                      .getOrElse({
                        inner(
                          Some(h)
                        )
                      })
                  })

              (extract, clm)
            })
            .getOrElse({

              val parentFuture =
                key
                  .parentOpt
                  .map(pk => get(pk))
                  .getOrElse(Future.successful(parentClassLoader))

              val jarFutures =
                key
                  .jarsSeq
                  .map({ jk =>
                    cache.getAsync(jk.uniqueId)
                  })



              val future : Future[ClassLoader] =
                for {
                  parent <- parentFuture
                  jars <- Future.sequence(jarFutures)
                } yield {
                  new ParentLastUrlClassloader(
                    jars.map({ jar =>
                      jar.toURI.toURL
                    }),
                    parent
                  )
                }

              future
                .onFailure({
                  case ex : Throwable =>
                    classLoaderMap
                      .transform(_ - key)
                })

              val trf =
                clm
                  .updated(
                    key,
                    new Holder(
                      future.map(WeakReference.apply)
                    )
                  )

              (future, trf)
            })

        })
    }

    inner(None)
  }

//  def get(
//    key: CaseClassLoaderKey
//  )(implicit
//    executionContext: ExecutionContext
//  ) : Future[ClassLoader] =  {
//    def load : Future[ClassLoader] = ???
//    classLoaderMap
//      .get(key)
//      .flatMap(_.get)
//      .getOrElse {
//
//        val parent =
//          key
//            .parentOpt
//            .map({ p =>
//              get(p)
//            })
//            .getOrElse(parentClassLoader)
//
//
//        val cl = new ParentLastUrlClassloader(
//          key.jarsSeq.map({ jar =>
//            cache.get(jar).toURI.toURL
//          }),
//          parent
//        )
//
//        classLoaderMap.put(key, WeakReference(cl))
//
//        cl
//      }
//  }


  def resolve[T](
    request: ClassRequest[T]
  ) : Future[T] = {
    for {
      cl <- get(CaseClassLoaderKey(request.classLoader))
    } yield {
      val runClass = cl.loadClass(request.className)
      val instance = runClass.newInstance().asInstanceOf[T]
      instance
    }
  }

  override def resolveAsync[T](request: ClassRequest[T]): AsyncValue[T] = {
    JavaImpl.wrapFuture(
      resolve(
        request
      )
    )
  }
}



