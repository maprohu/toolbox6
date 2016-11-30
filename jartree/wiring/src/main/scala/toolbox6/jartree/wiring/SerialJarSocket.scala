package toolbox6.jartree.wiring

import toolbox6.jartree.api.{ClassRequest, JarPlugger, JarSocket}

import scala.concurrent.Future

/**
  * Created by pappmar on 30/11/2016.
  */
class SerialJarSocket[T, C] extends JarSocket[T, C] {

  override def plugAsync(request: ClassRequest[JarPlugger[T, C]]): Future[T] = ???

  override def get(): T = ???

}
