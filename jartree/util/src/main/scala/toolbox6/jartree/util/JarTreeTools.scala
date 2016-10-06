package toolbox6.jartree.util

import javax.json.JsonObject

import toolbox6.jartree.api.{JarPlugResponse, JarPlugger, JarUpdatable}

/**
  * Created by Student on 06/10/2016.
  */
object JarTreeTools {

  def noopCleaner[T <: JarUpdatable, C](init: T) = new JarPlugger[T, C] {
    val response = new JarPlugResponse[T] {
      override def instance(): T = init
      override def andThen(): Unit = ()
    }
    override def pull(previous: T, param: JsonObject, context: C): JarPlugResponse[T] = response
  }

}
