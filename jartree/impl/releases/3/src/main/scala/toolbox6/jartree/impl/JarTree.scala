package toolbox6.jartree.impl


import java.io.File

import boopickle.Pickler
import com.typesafe.scalalogging.LazyLogging
import monix.execution.atomic.Atomic
import mvnmod.builder.{HasMavenCoordinates, MavenCoordinatesImpl, Module, ModulePath}
import toolbox6.jartree.api._
import toolbox6.pickling.PicklingTools

import scala.collection.mutable
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.ref.WeakReference

/**
  * Created by martonpapp on 27/08/16.
  */
object JarTree {

  val JarsPathElement = "jars"

  def jarClassLoaderFileName(
    maven: HasMavenCoordinates
  ) : String = {
    s"${JarTree.moduleClassLoaderName(maven)}.jar"
  }

  def jarClassLoaderResourcePath(
    maven: HasMavenCoordinates
  ) : Seq[String] = {
    Seq(
      JarsPathElement,
      jarClassLoaderFileName(maven)
    )
  }

  def embeddedKey(
    module: HasMavenCoordinates
  ) = {
    JarKey(
      module.toCanonical
    )
  }

  def embeddedJar(
    module: HasMavenCoordinates
  ) : EmbeddedJar = {
    val path = jarClassLoaderResourcePath(module).mkString("/")

    EmbeddedJar(
      classpathResource = path,
      key = embeddedKey(module)
    )

  }

  def moduleClassLoaderName(
    maven: HasMavenCoordinates
  ) : String = {
    s"${maven.groupId}_${maven.artifactId}_${maven.version}${maven.classifier.map(c => s"_${c}").getOrElse("")}"
  }

  def metaModule(
    module: Module
  ) = {
    MavenCoordinatesImpl(
      module.version.groupId,
      s"${module.version.artifactId}-meta",
      module.version.version
    )
  }

  def metaClassPath(
    module: Module
  ) : Seq[String] = {
    Seq(
      s"${moduleClassLoaderName(module.version)}.jartreemeta"
    )
  }

  def readMetaData[T](
    module: Module,
    classLoader: ClassLoader
  )(implicit
    pickler: Pickler[T]
  ) = {
    PicklingTools
      .fromInputStream[T]({ () =>
      classLoader.getResourceAsStream(metaClassPath(module).mkString("/"))
    })
  }

  def writeMetaData[T](
    module: Module,
    data: T,
    dir: File
  )(implicit
    pickler: Pickler[T]
  ) = {
    val metaFile = new File(
      dir,
      JarTree.metaClassPath(module).mkString("/")
    )
    metaFile.getParentFile.mkdirs()
    PicklingTools
      .toFile[T](
        data,
        metaFile
      )
  }

  def readMetaJarSeq(
    module: Module,
    classLoader: ClassLoader
  ) = {
    import boopickle.Default._
    readMetaData[JarSeq](
      module,
      classLoader
    )
  }

  def writeMetaJarSeq(
    module: Module,
    target: ModulePath,
    dir: File,
    keyProvider: HasMavenCoordinates => JarKey
  ) = {
    import boopickle.Default._
    writeMetaData[JarSeq](
      module,
      JarSeq(
        module
          .forTarget(
            target
          )
          .classPath
          .map(keyProvider)
      ),
      dir
    )
  }


}


class JarTree(
  val parentClassLoader: ClassLoader,
  val cache: JarCacheLike
)/*(implicit
  executionContext: ExecutionContext
)*/ extends ClassLoaderResolver with LazyLogging {
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
  )(implicit
    executionContext: ExecutionContext
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



