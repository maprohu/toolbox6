package toolbox6.jartree.util

import javax.json.JsonObject

import toolbox6.jartree.api.{JarPlugResponse, JarPlugger, JarUpdatable}

/**
  * Created by Student on 06/10/2016.
  */
object JarTreeTools {

  def noopResponse[T <: JarUpdatable](o: => T) = {
    new JarPlugResponse[T] {
      override def instance(): T = o
      override def andThen(): Unit = ()
    }
  }

  def noopCleaner[T <: JarUpdatable, C](init: T) = {
    val response = noopResponse(init)

    new JarPlugger[T, C] {
      override def pull(previous: T, param: JsonObject, context: C): JarPlugResponse[T] = response
    }
  }

}
