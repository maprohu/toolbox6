package toolbox6.jartree.wiring

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
import scala.concurrent.{Future, Promise}

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

class SimpleJarSocket[T <: JarUpdatable, C <: ScalaInstanceResolver](
  init: T,
  context: C,
  cleaner: JarPlugger[T, C],
  preProcessor: Option[PlugRequestImpl[T, C]] => Future[Unit] = (_:Option[PlugRequestImpl[T, C]]) => Future.successful()
)(implicit
  scheduler: Scheduler
) extends JarSocket[T, C] with LazyLogging {

  case class Input(
    request: Option[PlugRequestImpl[T, C]],
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
    request: Option[ClassRequestImpl[JarPlugger[T, C]]]
  )

  @volatile var currentInstance : Instance = Instance(init, None)

  val stopProcessor = subject
    .flatScan(State(currentInstance, None))({ (st, req) =>
      def pull(
        plugger: JarPlugger[T, C],
        param: Array[Byte],
        request: Option[ClassRequestImpl[JarPlugger[T, C]]]
      ) : Future[State] = {
        for {
          response <- JavaImpl.unwrapFuture(
            plugger.pullAsync(
              st.instance.instance,
              param,
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
          pull(cleaner, null, None)

        case (None, None) =>
          logger.warn(s"cleaning empty socket")
          Future.successful(st)

        case (Some(current), Some(pr)) if current == pr.request =>
          logger.info(s"updating: ${current}")
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

        case (copt, Some(pr)) =>
          logger.info(s"replacing: ${pr.request} (previous: ${copt})")
          for {
            plugger <- context.resolve(pr.request)
            _ = logger.info(s"resolved: ${plugger}")
            newState <- pull(plugger, pr.param, Some(pr.request))
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

  override def plugAsync(request: PlugRequest[T, C]): AsyncValue[T] = JavaImpl.wrapFuture(plug(request))

  val plugInput = BufferedSubscriber[Input](
    Subscriber(subject, scheduler),
    OverflowStrategy.Unbounded
  )

  def send(
    request: Option[PlugRequestImpl[T, C]]
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
    request: PlugRequest[T, C]
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

  def noCleaner[T <: JarUpdatable, C <: ScalaInstanceResolver](
    init: T,
    context: C
  )(implicit
    scheduler: Scheduler
  ) : SimpleJarSocket[T, C] = new SimpleJarSocket[T, C](
    init,
    context,
    JarTreeTools.noopCleaner[T, C](init)
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

  def noopClean[T <: JarUpdatable, C <: ScalaInstanceResolver](
    name: String,
    init: T
  )(implicit
    input: Input[C],
    scheduler: Scheduler
  ) : NamedSocket[T , C, SimpleJarSocket[T, C]] = {
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

object JarSocketTools {

//  def multiUpdate[T <: JarUpdatable, C, S <: JarSocket[T, C]](
//    param: JsonObject,
//    sockets: NamedSocket[T, C, S]*
//  ): Unit = {
//    sockets.foreach({ ns =>
//      val (request, p) =
//        JsonTools.readUpdate(
//          param.getJsonObject(ns.name)
//        )
//
//      ns.socket.plug(
//        request,
//        p
//      )
//    })
//  }

}
