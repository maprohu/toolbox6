package toolbox6.macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
/**
  * Created by Student on 07/10/2016.
  */
object Macros {

  def valName : String = macro impl

  def impl(c: Context): c.Expr[String] = {
    import c.universe._

    def call(enc: Symbol) : c.Expr[String] = {
      if (enc.isTerm) {
        val t = enc.asTerm
        if (t.isVal) {
          c.literal(t.name.toString)
        } else {
          call(enc.owner)
        }
      } else {
        call(enc.owner)
      }

    }

    call(c.internal.enclosingOwner)
  }

}
