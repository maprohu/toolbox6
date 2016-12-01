package toolbox6.common

import java.math.BigDecimal
import java.util.{GregorianCalendar, TimeZone}
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}

import scala.util.Try

/**
  * Created by pappmar on 10/10/2016.
  */
object Props extends Props {

}
trait Props {
  val UTC: TimeZone = TimeZone.getTimeZone("UTC")
  val dtf = DatatypeFactory.newInstance

  case class Prop[+T] (
    option : Option[T]
  ) {
    def map[V](fn: T => V) : Prop[V] = Prop(option.flatMap(o => Option(fn(o))))
    def flatMap[V](fn: T => Prop[V]) : Prop[V] = option.map(fn).getOrElse(Prop.Empty)
    def flatMapOption[V](fn: T => Option[V]) : Prop[V] = flatMap(o => Prop(fn(o)))
    def tryMap[V](fn: T => V) : Prop[V] = Try(map(fn)).getOrElse(Prop.Empty)
    def toInt(implicit ev: T => String) : Prop[Int] = map(ev(_).toInt)
    def orElse[V >: T](p: Prop[V]) : Prop[V] = if (option.isDefined) this else p
    def asString : Prop[String] = map(_.toString)
    def asInteger(implicit ev: T => Number) : Prop[java.lang.Integer] = map(ev(_).intValue().asInstanceOf[java.lang.Integer])
    def asBigDecimal(implicit ev: T => Number) : Prop[java.math.BigDecimal] = map(o => new BigDecimal(ev(o).toString()))
  }

  object Prop {
    val Empty = Prop(None)

    def apply[T](v : T) : Prop[T] = Prop(Option(v))
    implicit def toOption[T](prop: Prop[T]) : Option[T] = prop.option
  }

  implicit class OptionOps[T](option: Option[T]) {
    def toProp : Prop[T] = Prop(option)
  }

  def toXmlCalendar(time: Long) : XMLGregorianCalendar = {
    val cal = new GregorianCalendar(UTC)
    cal.setTimeInMillis(time)
    dtf.newXMLGregorianCalendar(cal)
  }


}
