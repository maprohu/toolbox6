package toolbox6.common

import monix.execution.atomic.Atomic

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by pappmar on 09/11/2016.
  */

trait Pooled[T] {
  def get: T
  def release : Future[Unit]
  def remove : Future[Unit]
}

trait Pool[T] {
  def get(implicit
    executionContext: ExecutionContext
  ) : Future[Pooled[T]]

  def close()(implicit
    executionContext: ExecutionContext
  ) : Future[Unit]
}

//object Pool {
//
//  def create[T](
//    factory: () => Future[T],
//    closer: T => Future[Unit],
//    size: Int = Int.MaxValue
//  ) = {
//    case class State(
//      in: List[T] = List.empty,
//      out: Seq[Pooled[T]] = Seq.empty
//    )
//    val state = Atomic(State())
//
//    new Pool[T] {
//      override def get(implicit executionContext: ExecutionContext): Future[Pooled[T]] = {
//        state.transformAndExtract({ s =>
//
//        })
//
//      }
//
//      override def close()(implicit executionContext: ExecutionContext): Future[Unit] = ???
//    }
//
//  }
//
//}
