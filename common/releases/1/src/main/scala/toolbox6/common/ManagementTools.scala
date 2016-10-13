package toolbox6.common

import javax.naming.{Context, InitialContext}

import monix.execution.Cancelable

import scala.util.Try

/**
  * Created by martonpapp on 02/10/16.
  */
object ManagementTools {

  def bind[T](
    name: String,
    instance: T
  ): Cancelable = {
    val ctx = new InitialContext()

    try {
      val nameElements =
        name
          .split('.')

      nameElements
        .init
        .foldLeft[Context](ctx)({ (acc, item) =>
          Try(
            acc.createSubcontext(item)
          ).getOrElse(
            acc.lookup(item).asInstanceOf[Context]
          )
        })
        .bind(nameElements.last, instance)

      Cancelable(() => unbind(name))

    } finally {
      ctx.close()
    }
  }

  def unbind(
    name: String
  ): Unit = {
    val ctx = new InitialContext()
    try {
      ctx.unbind(name)
    } finally {
      ctx.close()
    }
  }

}
