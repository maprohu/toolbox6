package toolbox6.jartree.wiring

import java.nio.ByteBuffer

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Scheduler
import monix.execution.atomic.Atomic
import monix.reactive.{Observable, OverflowStrategy}
import monix.reactive.observers.{BufferedSubscriber, Subscriber}
import monix.reactive.subjects.{PublishSubject, PublishToOneSubject}
import rx.{Rx, Var}
import toolbox6.jartree.api._
import toolbox6.jartree.util.JarTreeTools
//import toolbox6.jartree.impl.JarTreeTools
//import toolbox6.jartree.util.{JsonTools, ScalaInstanceResolver}
import toolbox6.logging.LogTools

import scala.concurrent.duration._
import scala.collection.immutable.Seq
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

//case class PlugRequest[T, C](
//  request: ClassRequestImpl[JarPlugger[T, C]]
////  param: Array[Byte]
//) extends PlugRequest[T, C]
//
//object PlugRequestImpl {
//  def apply[T, C](
//    request: PlugRequest[T, C]
//  ) : PlugRequestImpl[T, C] = {
//    PlugRequestImpl(
//      ClassRequestImpl(
//        request.request()
//      )
//    )
//  }
//
//
//}

case class Input[T, CtxApi](
  init: T,
  context: CtxApi,
  cleaner: JarPlugger[T, CtxApi],
  resolver: ClassLoaderResolver,
  classLoader: ClassLoader,
  preProcessor: Option[ClassRequest[JarPlugger[T, CtxApi]]] => Future[Unit] = (_:Any) => Future.successful()
)

class SimpleJarSocket[T, CtxApi](
  input: Input[T, CtxApi]
)(implicit
  scheduler: Scheduler
) extends JarSocket[T, CtxApi] with LazyLogging with LogTools {
  import input._

  case class Input(
    request: Option[ClassRequest[JarPlugger[T, CtxApi]]],
    promise: Promise[T]
  )

  private val subject = PublishToOneSubject[Input]()

  case class StateOut(
    cleanup: () => Unit
  )
  case class State(
    instance: Instance,
    out: Option[StateOut] = None
  )

  case class Instance(
    instance: T,
    request: Option[ClassRequest[JarPlugger[T, CtxApi]]]
  )

  @volatile var currentInstance : Instance = Instance(init, None)

  private val stopProcessor = subject
    .flatScan(State(currentInstance, None))({ (st, req) =>
      def pull(
        plugger: JarPlugger[T, CtxApi],
        pluggerClassLoader: ClassLoader,
        request: Option[ClassRequest[JarPlugger[T, CtxApi]]]
      ) : Future[State] = {
        for {
          response <-
            plugger.pull(
              PullParams(
                st.instance.instance,
                pluggerClassLoader,
                context
              )
            )
        } yield {
          val stInstance = Instance(
            response.instance,
            request
          )
          State(
            stInstance,
            Some(
              StateOut(
                { () =>
                  logger.info("calling plugging andThen")
                  response.andThen()
                  logger.info("plugging andThen complete")
                }
              )
            )
          )
        }
      }


      val future = (st.instance.request, req.request) match {
        case (Some(cr), None) =>
          logger.info(s"cleaning up: ${cr}")
          pull(cleaner, classLoader, None)

        case (None, None) =>
          logger.warn(s"cleaning empty socket")
          Future.successful(st)

        case (copt, or @ Some(pr)) =>
          logger.info(s"replacing: ${pr} (previous: ${copt})")
          for {
            pluggerClassLoader <- {
              resolver
                .resolve(pr.jars)
            }
            plugger = {
              JarTreeTools
                .instantiate[JarPlugger[T, CtxApi]](
                  pluggerClassLoader,
                  pr.className
                )
            }
            _ = logger.info(s"resolved: ${plugger}")
            newState <- pull(plugger, pluggerClassLoader,  or)
          } yield {
            newState
          }
      }

      Observable.fromFuture(
        preProcessor(req.request)
          .flatMap(_ => future)
          .map({ r =>
            currentInstance = r.instance
            req.promise.success(r.instance.instance)
            logger.info(s"plugging complete: ${r.instance.request}")
            r
          })
      )
    })
    .foreach({ st =>
      st.out.foreach(_.cleanup())
    })

  override def plugAsync(request: ClassRequest[JarPlugger[T, CtxApi]]) = plug(request)

  val plugInput = BufferedSubscriber[Input](
    Subscriber(subject, scheduler),
    OverflowStrategy.Unbounded
  )

  def send(
    request: Option[ClassRequest[JarPlugger[T, CtxApi]]]
  ) = {
    logger.info(s"plugging: ${request}")

    val promise = Promise[T]()

    subject
      .subscription
      .onComplete({ _ =>
        plugInput.onNext(
          Input(
            request,
            promise
          )
        )
      })

    promise.future

  }
  def plug(
    request: ClassRequest[JarPlugger[T, CtxApi]]
  ) : Future[T] = {
    send(
      Some(
        request
      )
    )
  }

  def get(): T = currentInstance.instance

  def query() = currentInstance.request

  def clear() = {
    send(None)
  }

  def stop() = {
    logger.info("clearing socket")
    quietly { Await.result(clear(), 30.second) }
    logger.info("stopping socket processor")
    subject.onComplete()
    quietly { Await.result(stopProcessor, 30.second) }
    logger.info("socket stopped")
  }

}

object SimpleJarSocket {

//  def noCleaner[T, CtxApi](
//    init: T,
//    context: CtxApi,
//    resolver: ClassLoaderResolver
//  )(implicit
//    scheduler: Scheduler
//  ) : SimpleJarSocket[T, CtxApi] = new SimpleJarSocket[T, CtxApi](
//    init,
//    context,
//    JarTreeTools.noopCleaner[T, CtxApi](init),
//    resolver
//  )

}


case class NamedSocket[T, C, S <: JarSocket[T, C]](
  name: String,
  socket: JarSocket[T, C]
)


object NamedSocket {

//  case class Input[C](
//    context: C,
//    resolver: ClassLoaderResolver
//  )

//  def noopClean[T, CtxApi](
//    name: String,
//    init: T
//  )(implicit
//    input: Input[CtxApi],
//    scheduler: Scheduler
//  ) : NamedSocket[T , CtxApi, SimpleJarSocket[T, CtxApi]] = {
//    import input._
//    NamedSocket(
//      name,
//      SimpleJarSocket.noCleaner(
//        init,
//        context,
//        resolver
//      )
//    )
//  }

}


