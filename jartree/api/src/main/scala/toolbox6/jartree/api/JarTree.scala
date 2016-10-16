package toolbox6.jartree.api

import java.io.InputStream


/**
  * Created by martonpapp on 27/08/16.
  */

//trait JarKey

trait JarKey {
  def uniqueId() : String
}

trait DeployableJar {
  def key: JarKey
  def data: InputStream
}

trait  ClassLoaderKey {
  def jars(): java.util.Collection[JarKey]
  def parent(): ClassLoaderKey
}

trait ClassRequest[+T] {
  def classLoader(): ClassLoaderKey
  def className(): String
}

//trait JarContext[X <: AnyRef] {
//
//  def deploy(jar: DeployableJar) : Unit
//  def setStartup(startup: ClassRequest[JarRunnable[X]]) : Unit
//  def extension(): X
//
//}

trait JarRunnable[C <: AnyRef] {

  def run(ctx: C, self: ClassRequest[JarRunnable[C]]) : Unit

}

trait InstanceResolver {
  def resolve[T](request: ClassRequest[T]) : T
}

trait JarRunnableByteArray[C <: AnyRef] {
  def run(data: Array[Byte], ctx: C, self: ClassRequest[JarRunnableByteArray[C]]) : Array[Byte]
}

trait JarPlugResponse[+T] {
  def instance() : T
  def andThen() : Unit
}

trait JarUpdatable {
  def update(param: Array[Byte]) : Unit
}

trait JarPlugger[T <: JarUpdatable, -C] {
  def pull(
    previous: T,
    param: Array[Byte],
    context: C
  ) : JarPlugResponse[T]
}


trait PlugRequest[T <: JarUpdatable, C] {
  def request(): ClassRequest[JarPlugger[T, C]]
  def param(): Array[Byte]
}

trait JarSocket[T <: JarUpdatable, C] {
  def plug(
    request: PlugRequest[T, C]
  ) : Unit

  def get() : T
}

trait Closable {
  def close() : Unit
}

trait ClosableJarPlugger[T <: JarUpdatable with Closable, C] extends JarPlugger[T, C] { self : T =>
  override def pull(previous: T, param: Array[Byte], context: C): JarPlugResponse[T] = {
    new JarPlugResponse[T] {
      override def instance(): T = self
      override def andThen(): Unit = previous.close()
    }
  }

}

class ClosableJarCleaner[T <: JarUpdatable with Closable](init: T) extends JarPlugger[T, Any] {
  override def pull(previous: T, param: Array[Byte], context: Any): JarPlugResponse[T] = {
    new JarPlugResponse[T] {
      override def instance(): T = init
      override def andThen(): Unit = previous.close()
    }
  }
}