package toolbox6.jartree.impl

import java.io.{File, FileOutputStream, InputStream, OutputStream}
import java.net.URLEncoder
import java.security.{DigestInputStream, MessageDigest}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.atomic.Atomic
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import toolbox6.jartree.api.{JarCacheLike, JarKey}
import toolbox6.jartree.util.CaseJarKey
import toolbox6.logging.LogTools

import scala.collection.immutable._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * Created by martonpapp on 27/08/16.
  */

class JarCache(
  val root: File
) extends JarCacheLike with LazyLogging with LogTools {

  root.mkdirs()


  var locks = Atomic(Map.empty[String, Future[File]])

  def getAsync(
    id: String
  ) : Future[File] = {
    locks
      .transformAndExtract({ l =>
        l
          .get(id)
          .map(f => (f, l))
          .getOrElse {
            val file = toManagedFile(id)

            if (file.exists()) {
              val fut = Future.successful(file)

              (fut, l.updated(id, fut))
            } else {
              (Future.failed(new Exception(s"not cached: ${id}")), l)
            }
          }
      })
  }

  def putAsync(
    id: String
  )(implicit
    executionContext: ExecutionContext
  ) : Option[(File, Promise[Unit])] = {
    locks
      .transformAndExtract { lock =>
        lock
          .get(id)
          .map({ _ =>
            (None, lock)
          })
          .getOrElse {
            val file = toManagedFile(id)

            if (file.exists()) {
              (None, lock.updated(id, Future.successful(file)))
            } else {
              val promise = Promise[Unit]()
              promise.future.onFailure({
                case ex =>
                  locks.transform({ lock =>
                    quietly {
                      file.delete()
                    }

                    lock - id
                  })
              })

              (
                Some(file, promise),
                lock.updated(id, promise.future.map(_ => file))
              )
            }
          }
      }
  }

  def delete(file: File) = synchronized {
    file.delete()
  }

//  def copyToCache(
//    jarFile: File,
//    in: InputStream
//  ) = {
//    try {
//      val out = new FileOutputStream(jarFile)
//      try {
//        IOUtils.copy(in, out)
//      } finally {
//        IOUtils.closeQuietly(in)
//        IOUtils.closeQuietly(out)
//      }
//      jarFile
//    } catch {
//      case ex : Throwable =>
//        delete(jarFile)
//        throw ex
//    }
//
//  }

  def toManagedFile(uniqueId: String) = {
    new File(
      root,
      s"${URLEncoder.encode(uniqueId, "UTF-8")}.jar"
    )
  }

  def get(
    hash: JarKey
  ) = {
    Await.result(getAsync(hash.uniqueId), Duration.Inf)
  }

  private def toJarFile(hash: JarKey) : File = {
    val file = toManagedFile(hash.uniqueId)
    file.getParentFile.mkdirs()
    file
  }

  def putStream(
    hash: JarKey,
    stream: () => InputStream
  )(implicit
    executionContext: ExecutionContext
  ) = {
    putAsync(hash.uniqueId)
      .foreach({
        case (jarFile, promise) =>
          promise.complete {
            Try {
              val in = stream()
              val out = new FileOutputStream(jarFile)
              try {
                IOUtils.copy(in, out)
              } finally {
                IOUtils.closeQuietly(in)
                IOUtils.closeQuietly(out)
              }
            }
          }
      })

  }

  def contains(id: String) : Boolean = {
    locks
      .transformAndExtract({ lock =>
        lock
          .get(id)
          .map({ _ =>
            (true, lock)
          })
          .getOrElse({
            val file = toManagedFile(id)
            if (file.exists()) {
              (true, lock.updated(id, Future.successful(file)))
            } else {
              (false, lock)
            }
          })
      })
  }

//  def put(
//    hash: CaseJarKey,
//    sourceFuture: Future[Source]
//  )(implicit
//    executionContext: ExecutionContext
//  ) : Unit = {
//    val jarFile = toJarFile(hash)
//
//    synchronized {
//      if (!locked.contains(jarFile) && !jarFile.exists()) {
//        locked = locked.updated(
//          jarFile,
//          sourceFuture
//            .map({ source =>
//              copyToCache(
//                jarFile,
//                source
//              )
//
//              jarFile
//            })
//        )
//      } else {
//        sourceFuture.onSuccess({
//          case source =>
//            try {
//              while (source.read() != -1) {}
//            } finally {
//              source.close()
//            }
//        })
//      }
//    }
//
//  }
//
//  def get(hash: CaseJarKey, source: Source) : Future[File] = {
//    val jarFile = toJarFile(hash)
//
//    val producer = synchronized {
//      locked
//        .get(jarFile)
//        .map(future => () => future)
//        .getOrElse({
//          if (jarFile.exists()) {
//            () => Future.successful(jarFile)
//          } else {
//            val promise = Promise[File]()
//
//            locked = locked.updated(jarFile, promise.future)
//
//            { () =>
//              promise.complete(
//                Try {
//                  copyToCache(
//                    jarFile,
//                    source
//                  )
//                }
//              )
//
//              Future.successful(jarFile)
//            }
//
//          }
//        })
//    }
//
//    producer()
//
//  }
//
//  def maybeGet(hash: CaseJarKey) : Option[Future[File]] = {
//    val jarFile = toJarFile(hash)
//
//    synchronized {
//      locked
//        .get(jarFile)
//        .orElse({
//          if (jarFile.exists()) {
//            Some(Future.successful(jarFile))
//          } else {
//            None
//          }
//        })
//    }
//  }

}

object JarCache {

  type Hash = Array[Byte]
  type Source = InputStream

  def createDigest = {
    MessageDigest.getInstance("SHA-256")
  }

  def createDigestInputStream(is: InputStream) = {
    new DigestInputStream(is, createDigest)
  }

  def calculateHash(in: Source) : Hash = {
    try {
      val digestInputStream = createDigestInputStream(in)
      IOUtils.copy(digestInputStream, new OutputStream {
        override def write(i: Int): Unit = ()
      })
      digestInputStream.getMessageDigest.digest()
    } finally {
      IOUtils.closeQuietly(in)
    }
  }

  def hashToString(hash: Hash) : String = {
    Base64.encodeBase64URLSafeString(hash)
  }

  def apply(root: File): JarCache = new JarCache(root)

}
