package toolbox6.jartree.api

import java.io.InputStream


/**
  * Created by martonpapp on 27/08/16.
  */

trait JarKey

trait HashJarKey extends JarKey {
  def hash() : Array[Byte]
}

trait MavenJarKey extends JarKey {
  def groupId() : String
  def artifactId() : String
  def version() : String
  def classifier() : String
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

//trait JarTreeRunner {
//  def cacheJar(
//    jarKey: JarKey
//  ) : JarCacheLoader
//
//  def run[T](
//    request: RunRequest,
//    callback: JarTreeRunnerCallback[T]
//  ) : Unit
//}
//
//trait JarTreeRunnerCallback[T] {
//  def resolved(instance: T) : Unit
//  def unresolved(jars: java.util.Collection[JarKey]) : Unit
//  def failure(ex: Throwable) : Unit
//}
//
//trait JarCacheLoader {
//  def success(is: InputStream) : Unit
//  def failure(ex: Throwable) : Unit
//
//}


trait JarRunning {
  def stop : Unit
}


trait JarContext[X <: AnyRef] {

  def deploy(jar: DeployableJar) : Unit
  def setStartup(startup: RunRequest) : Unit
  def extension: X

}



trait JarRunnable[X <: AnyRef] {

  def run(ctx: JarContext[X]) : JarRunning

}
