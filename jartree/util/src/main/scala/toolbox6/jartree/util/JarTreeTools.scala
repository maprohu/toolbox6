package toolbox6.jartree.util


import toolbox6.jartree.api.{JarPlugResponse, JarPlugger}

import scala.concurrent.Future

/**
  * Created by Student on 06/10/2016.
  */
object JarTreeTools {

  def noopResponse[T](o: => T) = {
    new JarPlugResponse[T] {
      override def instance(): T = o
      override def andThen(): Unit = ()
    }
  }


  def andThenResponse[T](o: => T, cb: () => Unit) = {
    new JarPlugResponse[T] {
      override def instance(): T = o
      override def andThen(): Unit = cb()
    }
  }


  def noopCleaner[T, C](init: T) = {
    val response = noopResponse(init)

    new JarPlugger[T, C] {
      override def pullAsync(previous: T, context: C) = Future.successful(response)
    }
  }

}
