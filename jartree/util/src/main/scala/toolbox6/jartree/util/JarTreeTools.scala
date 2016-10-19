package toolbox6.jartree.util


import toolbox6.jartree.api.{JarPlugResponse, JarPlugger, JarUpdatable}
import toolbox6.javaapi.AsyncValue
import toolbox6.javaimpl.JavaImpl

trait Closable {
  def close() : Unit
}
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

  def closableResponse[T <: JarUpdatable with Closable](o: => T, previous: T) = {
    new JarPlugResponse[T] {
      override def instance(): T = o
      override def andThen(): Unit = previous.close()
    }
  }


  def noopCleaner[T <: JarUpdatable, C](init: T) = {
    val response = noopResponse(init)

    new JarPlugger[T, C] {
//      override def pull(previous: T, param: Array[Byte], context: C): JarPlugResponse[T] = response
      override def pullAsync(previous: T, param: Array[Byte], context: C): AsyncValue[JarPlugResponse[T]] = JavaImpl.asyncSuccess(response)
    }
  }

}
