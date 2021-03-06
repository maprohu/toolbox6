package toolbox6.ui.ast

import scala.collection.immutable._
import scala.concurrent.Future


trait UI {

  def displaySync(widget: Widget) : Unit
  def run[T](fn: => T) : Future[T]

}


object UI {

  case object Default
    extends Size
      with Button.Position
      with Ability
      with SimpleHandler {
    override val simple: SimpleHandler.Type = () => ()
  }

}
sealed trait SimpleHandler {
  def simple: SimpleHandler.Type
}
object SimpleHandler {
  type Type = () => Unit
  case class of(
    simple: Type
  ) extends SimpleHandler

  implicit def toSimpleHandler(fn: Type) = of(fn)
  implicit def fromOption(fn: Option[Type]) : SimpleHandler = fn.map(toSimpleHandler).getOrElse(UI.Default)
}

sealed trait Widget

sealed trait Ability
object Ability {
  val Enabled = UI.Default
  case object Disabled extends Ability

  implicit def toBoolean(o: Ability) : Boolean = {
    o match {
      case Enabled => true
      case Disabled => false
    }
  }

  implicit def fromBoolean(o: Boolean) : Ability = {
    if (o) Enabled else Disabled
  }

}

sealed trait Size
case class Units(
  unit: Float = 1
) extends Size


case class SizedWidget[+W <: Widget](
  widget: W,
  size: Size = UI.Default
)

object SizedWidget {
  implicit def default[W <: Widget](w: W) : SizedWidget[W] = SizedWidget(w)
}


case class Column[+W <: Widget](
  widgets: SizedWidget[W]*
) extends Widget

object Button {
  sealed trait Position
  val Up = UI.Default
  case object Down extends Position
  object Position {
    implicit def toBoolean(p: Position) : Boolean = {
      p match {
        case Up => false
        case Down => true
      }
    }
  }
}
case class Button(
  label: String,
  ability: Ability = UI.Default,
  position: Button.Position = UI.Default,
  click: SimpleHandler = UI.Default
) extends Widget
