package toolbox6.akka.actor

import akka.Done
import akka.actor.{Actor, ActorRef, ActorRefFactory, ActorRefProvider, PoisonPill, Props, Stash}
import akka.actor.Actor.Receive
import akka.event.Logging

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import akka.pattern._
import akka.util.Timeout
import com.typesafe.scalalogging.{LazyLogging, StrictLogging}
import toolbox6.common.{Pool, Pooled}

import scala.util.Failure
import scala.concurrent.duration._

/**
  * Created by pappmar on 09/11/2016.
  */
class PoolActor[T](config: PoolActor.Config[T]) extends Actor with Stash {
  import PoolActor._
  val log = Logging(context.system, this)
  import config._
  import context.dispatcher
  implicit val _timeout : Timeout = timeout

  var state = State[T]()

  def ensureOpen(recipient: ActorRef = sender())(action: => Unit) : Unit = {
    if (state.closing) {
      recipient ! Failure(new Exception("pool is closed"))
    } else {
      action
    }
  }

  override def receive: Receive = {
    case Acquire =>
      ensureOpen() {
        self ! AcquireTo(sender())
      }
    case a : AcquireTo =>
      ensureOpen(a.recipient) {

        if (state.in.isEmpty) {
          stash()

          if (state.size < maxSize) {
            factory()
              .map({ item =>
                Created(item)
              })
              .pipeTo(self)

            state = state.copy(
              size = state.size + 1,
              creating = state.creating + 1
            )
          }
        } else {
          val item = state.in.head

          state = state.copy(
            in = state.in.tail,
            out = state.out :+ item
          )

          a.recipient ! item
        }
      }

    case c : Created[_] =>
      state = state.copy(
        in = c.item.asInstanceOf[T] +: state.in,
        creating = state.creating - 1
      )
      unstashAll()

    case r : Release[_] =>
      if (state.out.find(_ == r.item).isEmpty) {
        log.warning(s"releasing item which is not in pool: ${r.item}")
      }

      state = state.copy(
        in = r.item.asInstanceOf[T] +: state.in,
        out = state.out.diff(Seq(r.item))
      )
      unstashAll()

      sender ! Done

    case r : Remove[_] =>
      if (state.out.find(_ == r.item).isEmpty) {
        log.warning(s"removing item which is not in pool: ${r.item}")
      } else {
        state = state.copy(
          out = state.out.diff(Seq(r.item)),
          size = state.size - 1
        )
        unstashAll()
      }
      sender ! Done

    case Close =>
      if (state.creating > 0) {
        stash()
      } else {
        log.info("closing pool")

        if (!state.out.isEmpty) {
          log.warning(s"requesting pool close while ${state.out} items are leased")
        }

        Future
          .sequence(
            state
              .in
              .++(state.out)
              .map({ item =>
                closer(item)
                  .recover({
                    case ex =>
                      log.error(s"error closing pooled item: ${ex.getMessage}", ex)
                  })
              })
          )
          .map({ _ =>
            log.info("all items closed")
            PoisonPill
          })
          .pipeTo(self)

        state = State(closing = true)
      }

  }
}

object PoolActor extends StrictLogging {

  case class State[T](
    size: Int = 0,
    out: Seq[T] = Seq.empty,
    in: List[T] = List.empty,
    creating: Int = 0,
    closing : Boolean = false
  )

  case class Config[T](
    factory: () => Future[T],
    maxSize: Int = Int.MaxValue,
    closer: T => Future[Any],
    timeout: FiniteDuration = 15.seconds
  )

  case object Close
  case object Acquire
  case class AcquireTo(recipient : ActorRef)
  case class Release[T](item: T)
  case class Created[T](item: T)
  case class Remove[T](item: T)



  def create[T](
    config: Config[T]
  )(implicit
    actorRefProvider: ActorRefFactory
  ) : Pool[T] = {
    val ref = actorRefProvider.actorOf(
      Props(
        classOf[PoolActor[_]],
        config
      )
    )

    implicit val _timeout : Timeout = config.timeout

    new Pool[T] {
      override def get(implicit executionContext: ExecutionContext): Future[Pooled[T]] = {
        ref
          .ask(Acquire)
          .map({ item =>
            new Pooled[T] {
              override def get: T = item.asInstanceOf[T]
              override def release: Future[Unit] = {
                ref
                  .ask(Release(item))
                  .map(_ => ())
              }
              override def remove: Future[Unit] = {
                ref
                  .ask(Remove(item))
                  .map(_ => ())
              }
            }
          })
      }
      override def close()(implicit executionContext: ExecutionContext): Future[Unit] = {
        gracefulStop(
          ref,
          config.timeout,
          Close
        ).map({ r =>
          if (!r) {
            logger.warn("closing pool failed")
          }
          r
        })
      }
    }

  }

}
