package toolbox6.jartree.wiring

import monix.execution.atomic.Atomic
import rx.{Rx, Var}
import toolbox6.jartree.api._
import toolbox6.jartree.util.ClassRequestImpl


case class Plugged[T, C](
  instance: T,
  request: Option[ClassRequestImpl[JarPlugger[T, C]]]
)

class SimpleJarSocket[T, C](
  init: T,
  resolver: InstanceResolver,
  context: C,
  cleaner: JarPlugger[T, C]
) extends JarSocket[T, C] {

  private val initPlugged = Plugged[T, C](init, None)

  private val rxVar = Var(initPlugged)

  val current : Rx[Plugged[T, C]] = rxVar

  private val atomic = Atomic(
    initPlugged
  )

  override def get(): T = atomic.get.instance

  private val Noop = () => ()

  type PlugTransform = (() => Unit, Plugged[T, C])


  private def plugInternal2(
    plugged: Plugged[T, C],
    plugger: JarPlugger[T, C],
    request: Option[ClassRequestImpl[JarPlugger[T, C]]]
  ): PlugTransform = {
    val response = plugger.pull(
      plugged.instance,
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
    request: ClassRequest[JarPlugger[T, C]]
  ): Unit = {
    plugInternal1({ plugged =>
      val r = Some(ClassRequestImpl(request))

      if (plugged.request != r) {
        val plugger = resolver.resolve(request)

        plugInternal2(
          plugged,
          plugger,
          r
        )
      } else {
        (Noop, plugged)
      }
    })
  }


  def clear() = {
    plugInternal1({ plugged =>
      plugInternal2(
        plugged,
        cleaner,
        None
      )
    })
  }

}


