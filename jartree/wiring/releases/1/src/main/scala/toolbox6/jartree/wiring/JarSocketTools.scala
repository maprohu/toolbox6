package toolbox6.jartree.wiring

import javax.json.{JsonObject, JsonValue}

import monix.execution.atomic.Atomic
import rx.{Rx, Var}
import toolbox6.jartree.api._
import toolbox6.jartree.util.{ClassRequestImpl, JarTreeTools, JsonTools}

import scala.collection.immutable.Seq


case class Plugged[T <: JarUpdatable, C](
  instance: T,
  request: Option[(ClassRequestImpl[JarPlugger[T, C]], JsonObject)]
)

class SimpleJarSocket[T <: JarUpdatable, C <: InstanceResolver](
  init: T,
  context: C,
  cleaner: JarPlugger[T, C]
) extends JarSocket[T, C] {

  private val initPlugged = Plugged[T, C](init, None)

  private val rxVar = Var(initPlugged)

  val dynamic : Rx[Plugged[T, C]] = rxVar

  private val atomic = Atomic(
    initPlugged
  )

  override def get(): T = atomic.get.instance

  def query() = atomic.get.request

  private val Noop = () => ()

  type PlugTransform = (() => Unit, Plugged[T, C])


  private def plugInternal2(
    plugged: Plugged[T, C],
    plugger: JarPlugger[T, C],
    request: Option[(ClassRequestImpl[JarPlugger[T, C]], JsonObject)],
    param: JsonObject
  ): PlugTransform = {
    val response = plugger.pull(
      plugged.instance,
      param,
      context
    )

    val newPlugged =
      Plugged(
        response.instance(),
        request
      )

    rxVar() = newPlugged

    (
      () => response.andThen(),
      newPlugged
    )
  }

  private def plugInternal1(
    trf: Plugged[T, C] => PlugTransform
  ): Unit = {
    val andThen = atomic.transformAndExtract({ plugged =>
      trf(plugged)
    })

    andThen()
  }

  override def plug(
    request: ClassRequest[JarPlugger[T, C]],
    param: JsonObject
  ): Unit = {
    plugInternal1({ plugged =>
      val r = Some(ClassRequestImpl(request), param)

      if (plugged.request.map(_._1) != r.map(_._1)) {
        val plugger = context.resolve(request)

        plugInternal2(
          plugged,
          plugger,
          r,
          param
        )
      } else {
        plugged.instance.update(param)

        (Noop, plugged.copy(request = r))
      }
    })
  }


  def clear() = {
    plugInternal1({ plugged =>
      plugInternal2(
        plugged,
        cleaner,
        None,
        null
      )
    })
  }

}

object SimpleJarSocket {

  def noCleaner[T <: JarUpdatable, C <: InstanceResolver](
    init: T,
    context: C
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
    context: C
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

  def multiUpdate[T <: JarUpdatable, C, S <: JarSocket[T, C]](
    param: JsonObject,
    sockets: NamedSocket[T, C, S]*
  ): Unit = {
    sockets.foreach({ ns =>
      val (request, p) =
        JsonTools.readUpdate(
          param.getJsonObject(ns.name)
        )

      ns.socket.plug(
        request,
        p
      )
    })
  }

}