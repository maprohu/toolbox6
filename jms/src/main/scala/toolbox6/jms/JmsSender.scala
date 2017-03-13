package toolbox6.jms

import javax.jms._
import javax.naming.InitialContext

import com.typesafe.scalalogging.LazyLogging
import monix.execution.atomic.Atomic
import toolbox6.logging.LogTools

import scala.util.{Failure, Success, Try}

/**
  * Created by martonpapp on 06/10/16.
  */

case class JmsSender[T](
  sender: T => Try[Unit],
  close: () => Unit
)

object JmsSender extends LazyLogging with LogTools {

  case class JmsConnection(
    connection: Connection,
    session: Session,
    producer: MessageProducer
  ) {
    def close() : Unit = {
      quietly(producer.close())
      quietly(session.close())
      quietly(connection.close())
    }
  }

  type Connector = () => (ConnectionFactory, Destination)
  type Creator[T] = (Session, T) => Message

  def text[T](fn: T => String) : Creator[T] = { (session, item) =>
    session.createTextMessage(fn(item))
  }

  type Headers = Map[String, String]

  case class TextWithHeaders(
    text: String,
    headers: Headers
  )

  def textWithHeaders[T](fn: T => String) : Creator[(T, Headers)] = { (session, item) =>
    val msg = session.createTextMessage(fn(item._1))
    item._2.foreach({
      case (key, value) => msg.setStringProperty(key, value)
    })
    msg
  }

  val TextWithHeadersCreator : Creator[TextWithHeaders] = { (session, item) =>
    val msg = session.createTextMessage(item.text)
    item.headers.foreach({
      case (key, value) => msg.setStringProperty(key, value)
    })
    msg
  }

  def jndi(
    cfn: String,
    dn: String,
    icp : () => InitialContext
  ) : Connector = { () =>
    val ctx = icp()
    try {
      val cf = ctx.lookup(cfn).asInstanceOf[ConnectionFactory]
      val d = ctx.lookup(dn).asInstanceOf[Destination]
      (cf, d)
    } finally {
      ctx.close()
    }
  }

  def sender[T](
    connector: Connector,
    creator: Creator[T]
  ) : JmsSender[T] = {
    val atomic = Atomic(Option.empty[JmsConnection])

    JmsSender[T](
      sender = { item =>
        atomic.transformAndExtract({ copt =>
          try {
            val c = copt.getOrElse({
              val (cf, d) = connector()
              val connection = cf.createConnection()
              val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
              val producer = session.createProducer(d)

              JmsConnection(connection, session, producer)
            })

            try {
              val msg = creator(c.session, item)
              c.producer.send(msg)
              (Success(), Some(c))
            } catch {
              case ex : Throwable =>
                c.close()
                (Failure(ex), None)
            }
          } catch {
            case ex : Throwable =>
              (Failure(ex), None)
          }
        })
      },
      close = { () =>
        atomic.transform({ c =>
          c.foreach(_.close())
          None
        })
      }
    )
  }

}
