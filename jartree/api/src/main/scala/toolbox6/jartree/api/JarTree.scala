package toolbox6.jartree.api

import java.io.File

import scala.concurrent.Future
import scala.collection.immutable._


case class JarKey(
  uniqueId: String
)

case class ClassRequest[+T](
  jars: Seq[JarKey],
  className: String
)

trait InstanceResolver {
  def resolveAsync[T](request: ClassRequest[T]) : Future[T]
}

trait JarPlugResponse[+T] {
  def instance() : T
  def andThen() : Unit
}

trait JarPlugger[T, -C] {
  def pullAsync(
    previous: T,
    context: C
  ) : Future[JarPlugResponse[T]]
}


case class PlugRequest[T, -C] (
  request: ClassRequest[JarPlugger[T, C]]
)

trait JarSocket[T, C] {
  def plugAsync(
    request: PlugRequest[T, C]
  ) : Future[T]

  def get() : T
}


trait JarCacheLike {

  def getAsync(
    id: String
  ) : Future[File]

}