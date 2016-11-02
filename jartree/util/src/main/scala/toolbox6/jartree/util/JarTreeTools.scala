package toolbox6.jartree.util

import com.typesafe.scalalogging.LazyLogging
import toolbox6.jartree.api.{ClassLoaderResolver, ClassRequest, JarPlugResponse, JarPlugger}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Student on 06/10/2016.
  */
object JarTreeTools extends LazyLogging {

  def noopResponse[T](o: T) = {
    JarPlugResponse[T](o, () => ())
  }


  def andThenResponse[T](o: T, cb: () => Unit) = {
    JarPlugResponse[T](o, cb)
  }


//  def noopCleaner[T, C](init: T) = {
//    val response = noopResponse(init)
//
//    new JarPlugger[T, C] {
//      override def pull(previous: T, classLoader: ClassLoader, context: C) = Future.successful(response)
//    }
//  }


  def instantiate[T](
    cl: ClassLoader,
    className: String
  ) : T = {
    val runClass = cl.loadClass(className)
    runClass.newInstance().asInstanceOf[T]
  }

  def resolve[T](
    resolver: ClassLoaderResolver,
    request: ClassRequest[T]
  )(implicit
    executionContext: ExecutionContext
  ) : Future[T] = {
    logger.info(s"resolving: ${request}")

    for {
      cl <- resolver.resolve(request.jars)
    } yield {
      logger.info(s"instantiating: ${request}")
      val instance = instantiate[T](
        cl,
        request.className
      )
      logger.info(s"instantiated: ${instance}")
      instance
    }
  }

}
