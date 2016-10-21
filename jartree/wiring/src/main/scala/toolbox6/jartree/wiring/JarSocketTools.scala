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
import toolbox6.jartree.util.{ClassRequestImpl, JarTreeTools, JsonTools, ScalaInstanceResolver}
import toolbox6.javaapi.AsyncValue
import toolbox6.javaimpl.JavaImpl

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future, Promise}

case class PlugRequestImpl[T <: JarUpdatable, C](
  request: ClassRequestImpl[JarPlugger[T, C]],
  param: Array[Byte]
) extends PlugRequest[T, C]

object PlugRequestImpl {
  def apply[T <: JarUpdatable, C](
    request: PlugRequest[T, C]
  ) : PlugRequestImpl[T, C] = {
    PlugRequestImpl(
      ClassRequestImpl(
        request.request()
      ),
      request.param()
    )
  }


}


//case class Plugged[T <: JarUpdatable, C](
//  instance: Future[T],
//  request: Option[PlugRequestImpl[T, C]]
//)

class SimpleJarSocket[T <: JarUpdatable, CtxApi <: InstanceResolver, Context <: CtxApi with ScalaInstanceResolver](
  init: T,
  context: Context,
  cleaner: JarPlugger[T, CtxApi],
  preProcessor: Option[PlugRequestImpl[T, CtxApi]] => Future[Unit] = (_:Option[PlugRequestImpl[T, CtxApi]]) => Future.successful()
)(implicit
  scheduler: Scheduler
) extends JarSocket[T, CtxApi] with LazyLogging {

  case class Input(
    request: Option[PlugRequestImpl[T, CtxApi]],
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
    request: Option[PlugRequestImpl[T, CtxApi]]
  )

  @volatile var currentInstance : Instance = Instance(init, None)

  val stopProcessor = subject
    .flatScan(State(currentInstance, None))({ (st, req) =>
      def pull(
        plugger: JarPlugger[T, CtxApi],
//        param: Array[Byte],
        request: Option[PlugRequestImpl[T, CtxApi]]
      ) : Future[State] = {
        for {
          response <- JavaImpl.unwrapFuture(
            plugger.pullAsync(
              st.instance.instance,
//              request.map(_.param).orNull,
              context
            )
          )
        } yield {
          val instance = response.instance()
          val stInstance = Instance(
            instance,
            request
          )
          State(
            stInstance,
            Some(
              StateOut(
                () => response.andThen()
              )
            )
          )
        }
      }


      val future = (st.instance.request, req.request) match {
        case (Some(cr), None) =>
          logger.info(s"cleaning up: ${cr}")
          pull(cleaner, None)

        case (None, None) =>
          logger.warn(s"cleaning empty socket")
          Future.successful(st)

        case (Some(current), Some(pr)) if current.request == pr.request =>
          logger.info(s"updating: ${current.request}")
          for {
            _ <- JavaImpl.unwrapFuture(
              st
                .instance
                .instance
                .updateAsync(
                  pr.param
                )
            )
          } yield {
            st
          }

        case (copt, or @ Some(pr)) =>
          logger.info(s"replacing: ${pr.request} (previous: ${copt})")
          for {
            plugger <- context.resolve(pr.request)
            _ = logger.info(s"resolved: ${plugger}")
            newState <- pull(plugger, or)
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

//  private val initPlugged = Plugged[T, C](init, None)
//
//  private val rxVar = Var(initPlugged)
//
//  val dynamic : Rx[Plugged[T, C]] = rxVar
//
//  private val atomic = Atomic(
//    initPlugged
//  )

  override def plugAsync(request: PlugRequest[T, CtxApi]): AsyncValue[T] = JavaImpl.wrapFuture(plug(request))

  val plugInput = BufferedSubscriber[Input](
    Subscriber(subject, scheduler),
    OverflowStrategy.Unbounded
  )

  def send(
    request: Option[PlugRequestImpl[T, CtxApi]]
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
    request: PlugRequest[T, CtxApi]
  ) : Future[T] = {
    send(
      Some(
        PlugRequestImpl(
          request
        )
      )
    )
  }

  def get(): T = currentInstance.instance

  def query() = currentInstance.request

  def clear() = {
    send(None)
  }

}

object SimpleJarSocket {

  def noCleaner[T <: JarUpdatable, CtxApi <: InstanceResolver, Context <: CtxApi with ScalaInstanceResolver](
    init: T,
    context: Context
  )(implicit
    scheduler: Scheduler
  ) : SimpleJarSocket[T, CtxApi, Context] = new SimpleJarSocket[T, CtxApi, Context](
    init,
    context,
    JarTreeTools.noopCleaner[T, CtxApi](init)
  )

}


case class NamedSocket[T <: JarUpdatable, C, S <: JarSocket[T, C]](
  name: String,
  socket: JarSocket[T, C]
)


object NamedSocket {

  case class Input[C <: ScalaInstanceResolver](
    context: C
  )

  def noopClean[T <: JarUpdatable, CtxApi <: InstanceResolver, Context <: CtxApi with ScalaInstanceResolver](
    name: String,
    init: T
  )(implicit
    input: Input[Context],
    scheduler: Scheduler
  ) : NamedSocket[T , CtxApi, SimpleJarSocket[T, CtxApi, Context]] = {
    import input._
    NamedSocket(
      name,
      SimpleJarSocket.noCleaner(
        init,
        context
      )
    )
  }

}

case class MultiUpdate[T <: JarUpdatable, C](
  bindings: Map[String, PlugRequestImpl[T, C]]
)

object JarSocketTools {

  def multiUpdateAsync[T <: JarUpdatable, C, S <: JarSocket[T, C]](
    param: Array[Byte],
    sockets: NamedSocket[T, C, S]*
  )(implicit
    executionContext: ExecutionContext
  ): AsyncValue[Unit] = {
    import boopickle.Default._

    val updates = Unpickle[MultiUpdate[T, C]].fromBytes(
      ByteBuffer.wrap(param)
    )

    JavaImpl.wrapFuture(
      Future
        .sequence(
          sockets
            .map({ socket =>
              JavaImpl.unwrapFuture(
                socket.socket.plugAsync(
                  updates.bindings(socket.name)
                )
              )
            })
        )
        .map(_ => ())
    )
  }

}
