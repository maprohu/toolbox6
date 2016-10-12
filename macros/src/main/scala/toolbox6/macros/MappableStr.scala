package toolbox6.macros


import java.util.Date

import scala.language.experimental.macros
import scala.reflect.api.Universe
import scala.reflect.macros.whitebox.Context

trait MappableStr[T] {
  def fromMap(map: Map[String, String]): T
}

object MappableStr {



  implicit def materializeMappable[T]: MappableStr[T] =
    macro materializeMappableImpl[T]

  def materializeMappableImpl[T: c.WeakTypeTag](c: Context): c.Expr[MappableStr[T]] = {
    import c.universe._
    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val fromMapParams = extract[T](c.universe)

    c.Expr[MappableStr[T]] { q"""
      new MappableStr[$tpe] {
        def fromMap(map: Map[String, String]): $tpe = $companion(..$fromMapParams)
      }
    """ }
  }


  def extract[T: ru.WeakTypeTag](ru: Universe)(implicit
    parseDate: String => Date
  ) = {
    import ru._
    val tpe = ru.weakTypeOf[T]
    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head

    def extractField(from: ru.Tree, rt: ru.Type) = {
      rt match {
        case t if t == typeOf[String] =>
          q"$from"
        case t if t == typeOf[Int] =>
          q"$from.toInt"
        case t if t == typeOf[Double] =>
          q"$from.toDouble"
        case t if t.typeSymbol == typeOf[Date].typeSymbol =>
          q"MappableStr.parseDate($from)"
        case _ => ???
      }

    }


    fields.map { field ⇒
      val name = field.asTerm.name
      val decoded = name.decodedName.toString
      val returnType = tpe.decl(name).typeSignature
      val rt = returnType.asInstanceOf[NullaryMethodTypeApi].resultType
      val e = rt.erasure
      val d = typeOf[Date]
      println(e)

      rt match {
        case o if o.erasure == typeOf[Option[Any]] =>
          println(o)
          q"map.get($decoded).map(o => ${extractField(q"o", o.typeArgs.head)})"
        case o =>
          extractField(q"map($decoded)", o)
        case _ => ???
      }
    }


  }
}