package toolbox6.macros


import java.util.{Calendar, Date, TimeZone}
import javax.xml.bind.DatatypeConverter

import scala.language.experimental.macros
import scala.reflect.api.Universe
import scala.reflect.macros.whitebox.Context

trait Conversions {
  def stringToDate(s: String) : Date
  def dateToString(s: Date) : String
}

trait MappableStr[T] {
  def fromMap(map: Map[String, String])(implicit conversions: Conversions = DefaultConversions): T
  def toMap(o: T)(implicit conversions: Conversions = DefaultConversions): scala.collection.immutable.Map[String, String]
}

trait DefaultConversions extends Conversions {
  override def stringToDate(s: String): Date = new Date(DatatypeConverter.parseDateTime(s).getTimeInMillis)

  val UTC = TimeZone.getTimeZone("UTC")
  override def dateToString(s: Date): String = {
    val cal = Calendar.getInstance(UTC)
    cal.setTime(s)
    DatatypeConverter.printDateTime(cal)
  }
}

object DefaultConversions extends DefaultConversions

object MappableStr {


  def checkKnownNames(
    input: Map[String, String],
    knownParams: Set[String]
  ) = {
    val unknown = input.keySet -- knownParams

    require(unknown.isEmpty, s"Unknown key${if (unknown.size > 1) "s" else ""}: ${unknown.mkString(",")}")
  }


  implicit def materializeMappable[T]: MappableStr[T] =
    macro materializeMappableImpl[T]

  def materializeMappableImpl[T: c.WeakTypeTag](c: Context): c.Expr[MappableStr[T]] = {
    import c.universe._
    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val (names, fromMapParams, toMapParams) = extract[T](c.universe).unzip3

    c.Expr[MappableStr[T]] { q"""
      new MappableStr[$tpe] {
        val known =
          Set(
            ..$names
          )

        def toMap(o: $tpe)(implicit conversions: _root_.toolbox6.macros.Conversions = _root_.toolbox6.macros.DefaultConversions): scala.collection.immutable.Map[String, String] = {
          _root_.toolbox6.macros.MappableStr.mapOfOptions(..$toMapParams)
        }
        def fromMap(map: Map[String, String])(implicit conversions: _root_.toolbox6.macros.Conversions = _root_.toolbox6.macros.DefaultConversions): $tpe = {
          MappableStr.checkKnownNames(
            map,
            known
          )
          $companion(..$fromMapParams)
        }
      }
    """ }
  }

  def mapOfOptions[K, V](
    values: (K, Option[V])*
  ) : scala.collection.immutable.Map[K, V] = {
    values
      .flatMap({
        case (k, vopt) =>
          vopt.map(v => k -> v)
      })
      .toMap
  }


  def extract[T: ru.WeakTypeTag](ru: Universe) = {
    import ru._
    val tpe = ru.weakTypeOf[T]
    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head

    def extractField(from: ru.Tree, to: ru.Tree, rt: ru.Type) = {
      rt match {
        case t if t == typeOf[String] =>
          (q"$from", to)
        case t if t == typeOf[Int] =>
          (q"$from.toInt", q"$to.toString")
        case t if t == typeOf[Double] =>
          (q"$from.toDouble", q"$to.toString")
        case t if t == typeOf[Float] =>
          (q"$from.toFloat", q"$to.toString")
        case t if t.typeSymbol == typeOf[Date].typeSymbol =>
          (q"""conversions.stringToDate($from)""", q"conversions.dateToString($to)")
        case _ => ???
      }

    }


    fields.map { field ⇒
      val name = field.asTerm.name
      val decoded = name.decodedName.toString
      val returnType = tpe.decl(name).typeSignature
      val rt = returnType.asInstanceOf[NullaryMethodTypeApi].resultType

      val (from, to) = rt match {
        case o if o.erasure == typeOf[Option[Any]] =>
          val (ofrom, oto) = extractField(q"o", q"o", o.typeArgs.head)
          (q"map.get($decoded).map(o => ${ofrom})", q"$decoded -> o.$name.map(o => ${oto})")
        case o =>
          val (ofrom, oto) = extractField(q"map($decoded)", q"o.$name", o)
          (ofrom, q"$decoded -> Some($oto)")
        case _ => ???
      }

      (
        q"$decoded",
        q"""
            try {
              $from
            } catch {
              case _root_.scala.util.control.NonFatal(ex) =>
                throw new RuntimeException("Error processing field: " + $decoded, ex)
            }
        """,
        to
      )
    }


  }
}