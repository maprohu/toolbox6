package toolbox6.jartree.wiring

import monix.execution.Scheduler
import monix.execution.atomic.Atomic
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
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

class SimpleJarSocket[T <: JarUpdatable, C <: InstanceResolver](
  init: T,
  context: C with ScalaInstanceResolver,
  cleaner: JarPlugger[T, C]
)(implicit
  scheduler: Scheduler
) extends JarSocket[T, C] {

  case class Input(
    request: Option[PlugRequestImpl[T, C]],
    promise: Promise[T]
  )

  private val subject = PublishSubject[Input]()

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
      def respond(i: T) = {
        req.promise.success(i)
      }
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
          currentInstance = stInstance
          req.promise.success(instance)
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
        case (Some(_), None) =>
          pull(cleaner, null, None)

        case (None, None) =>
          respond(st.instance.instance)
          Future.successful(st)

        case (Some(current), Some(pr)) if current == pr.request =>
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
            respond(st.instance.instance)
            st
          }

        case (_, Some(pr)) =>
          for {
            plugger <- context.resolve(pr.request)
            newState <- pull(plugger, pr.param, Some(pr.request))
          } yield {
            newState
          }
      }

      Observable.fromFuture(future)
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

  override def plugAsync(request: PlugRequest[T, C]): AsyncValue[Unit] = JavaImpl.wrapFuture(plug(request))

  def plug(
    request: PlugRequest[T, C]
  ) : Future[Unit] = {
    val promise = Promise[Unit]()



    promise.future
  }

  def get(): T = currentInstance.instance

  def query() = currentInstance.request

  private val Noop = () => ()

//  type PlugTransform = (() => Unit, Plugged[T, C])
//
//
//  private def plugInternal2(
//    plugged: Plugged[T, C],
//    plugger: JarPlugger[T, C],
//    request: Option[PlugRequestImpl[T, C]],
//    param: Array[Byte]
//  ): PlugTransform = {
//    val response = plugger.pull(
//      plugged.instance,
//      param,
//      context
//    )
//
//    val newPlugged =
//      Plugged(
//        response.instance(),
//        request
//      )
//
//    rxVar() = newPlugged
//
//    (
//      () => response.andThen(),
//      newPlugged
//    )
//  }
//
//  private def plugInternal1(
//    trf: Plugged[T, C] => PlugTransform
//  ): Unit = {
//    val andThen = atomic.transformAndExtract({ plugged =>
//      trf(plugged)
//    })
//
//    andThen()
//  }
//
//  def plug(
//    request: PlugRequest[T, C]
//  ): Future[Unit] = {
//    plugInternal1({ plugged =>
//      val r = Some(PlugRequestImpl(request))
//
//      if (plugged.request.map(_.request) != r.map(_.request)) {
//        val plugger = context.resolve(request.request())
//
//        plugInternal2(
//          plugged,
//          plugger,
//          r,
//          request.param
//        )
//      } else {
//        plugged.instance.update(request.param())
//
//        (Noop, plugged.copy(request = r))
//      }
//    })
//  }
//
//
//  def clear() = {
//    plugInternal1({ plugged =>
//      plugInternal2(
//        plugged,
//        cleaner,
//        None,
//        null
//      )
//    })
//  }

}

object SimpleJarSocket {

  def noCleaner[T <: JarUpdatable, C <: InstanceResolver](
    init: T,
    context: C with ScalaInstanceResolver
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

  case class Input[C <: InstanceResolver](
    context: C with ScalaInstanceResolver
  )

  def noopClean[T <: JarUpdatable, C <: InstanceResolver](
    name: String,
    init: T
  )(implicit
    input: Input[C]
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
