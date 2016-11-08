package toolbox6.jartree.api

import java.io.File
import java.nio.file.Path

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable._


case class JarKey(
  uniqueId: String
)

case class JarSeq(
  jars: Seq[JarKey]
)

case class ClassRequest[+T](
  jars: JarSeq,
  className: String
)

object ClassRequest {
  def apply[T](
    jars: Seq[JarKey],
    className: String
  ) : ClassRequest[T] = {
    ClassRequest[T](
      JarSeq(jars),
      className
    )
  }
}


trait ClassLoaderResolver {
  def resolve(
    request: JarSeq
  )(implicit
    executionContext: ExecutionContext
  ) : Future[ClassLoader]
}

case class JarPlugResponse[+T](
  instance: T,
  andThen: () => Unit
)

case class PullParams[T, +C](
  previous: T,
  classLoader: ClassLoader,
  context: C
)
trait JarPlugger[T, -C] {
  def pull(
    params: PullParams[T, C]
  ) : Future[JarPlugResponse[T]]
}


//case class PlugRequest[T, -C] (
//  request: ClassRequest[JarPlugger[T, C]]
//)

trait JarSocket[T, +C] {
  def plugAsync(
    request: ClassRequest[JarPlugger[T, C]]
  ) : Future[T]

  def get() : T
}


trait JarCacheLike {

  def getAsync(
    id: String
  ) : Future[File]

}

case class JarTreeContext(
  name: String,
  log: Option[File],
  storage: Option[File],
  cache: JarCacheLike
)