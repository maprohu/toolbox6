package toolbox6.jartree.impl


import com.typesafe.scalalogging.LazyLogging
import monix.execution.atomic.Atomic
import toolbox6.jartree.api._

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
  val cache: JarCacheLike
)(implicit
  executionContext: ExecutionContext
) extends ClassLoaderResolver with LazyLogging {
  type CaseClassLoaderKey = Seq[JarKey]

  class Holder(
    val future : Future[WeakReference[ClassLoader]]
  )
  private val classLoaderMap = Atomic(Map.empty[CaseClassLoaderKey, Holder])

  def clear() = {
    classLoaderMap.set(Map.empty)
  }

  override def resolve(
    jars: JarSeq
  ) : Future[ClassLoader] = {
    val key = jars.jars

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
              logger.info(s"creating classloader: ${key}")

//              val parentFuture =
//                key
//                  .parentOpt
//                  .map(pk => get(pk))
//                  .getOrElse(Future.successful(parentClassLoader))

              val jarFutures =
                key
                  .map({ jk =>
                    cache.getAsync(jk.uniqueId)
                  })



              val future : Future[ClassLoader] =
                for {
                  jars <- Future.sequence(jarFutures)
                } yield {
                  new ParentLastUrlClassloader(
                    jars.map({ jar =>
                      jar.toURI.toURL
                    }),
                    parentClassLoader
                  )
                }

              future
                .onFailure({
                  case ex : Throwable =>
                    logger.error(s"error loading: ${key}", ex)
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


//  def resolve[T](
//    request: ClassRequest[T]
//  ) : Future[T] = {
//    logger.info(s"resolving: ${request}")
//
//    for {
//      cl <- get(request.jars)
//    } yield {
//      logger.info(s"instantiating: ${request}")
//      val runClass = cl.loadClass(request.className)
//      val instance = runClass.newInstance().asInstanceOf[T]
//      logger.info(s"instantiated: ${instance}")
//      instance
//    }
//  }
//
//  override def resolve[T](request: ClassRequest[T]) = {
//    resolve(
//      request
//    )
//  }
}



