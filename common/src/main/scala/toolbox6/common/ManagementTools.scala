package toolbox6.common

import java.rmi.Remote
import java.rmi.server.UnicastRemoteObject
import javax.naming.{Context, InitialContext}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Cancelable
import toolbox6.logging.LogTools

import scala.util.Try

/**
  * Created by martonpapp on 02/10/16.
  */
object ManagementTools extends LazyLogging with LogTools {

  def bind[T <: Remote](
    existing: Seq[String],
    path: Seq[String],
    name: String,
    instance: T
  ): Cancelable = {
    def root(c: Context) =
      existing
        .foldLeft(c)( (acc, elem) =>
          acc.lookup(elem).asInstanceOf[Context]
        )

    val ctx = new InitialContext()

    try {

      val container =
        path
          .foldLeft(root(ctx))({ (acc, elem) =>
            try {
              acc.createSubcontext(elem)
            } catch {
              case _ : Throwable =>
                acc.lookup(elem).asInstanceOf[Context]
            }
          })

      container
        .bind(name, instance)


      Cancelable({ () =>
        quietly {
          val ctx = new InitialContext()

          try {
            val (p, c) = path
              .foldLeft((Seq.empty[(Context, String)], root(ctx)))({ (acc, elem) =>
                val (p, c) = acc
                val c2 = c.lookup(elem).asInstanceOf[Context]
                (p :+ (c, elem), c2)
              })

            c.unbind(name)

//            p
//              .reverse
//              .foreach {
//                case (c, n) =>
//                  c.destroySubcontext(n)
//              }
          } finally {
            ctx.close()
          }


        }
//        quietly { UnicastRemoteObject.unexportObject(instance, true) }
      })

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
