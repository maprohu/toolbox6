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
  def jar(): JarKey
  def dependencies(): java.util.Collection[ClassLoaderKey]
}

trait RunRequest {
  def classLoader(): ClassLoaderKey
  def className(): String
}

trait JarContext[X <: AnyRef] {

  def deploy(jar: DeployableJar) : Unit
  def setStartup(startup: RunRequest) : Unit
  def extension(): X

}

trait JarRunnable[X <: AnyRef] {

  def run(ctx: JarContext[X], self: ClassLoaderKey) : Unit

}

trait JarRunnableByteArray[X <: AnyRef] {
  def run(data: Array[Byte], ctx: JarContext[X], self: ClassLoaderKey) : Array[Byte]
}
