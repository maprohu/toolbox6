package toolbox6.jms

import javax.jms._

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.typesafe.scalalogging.LazyLogging
import monix.execution.FutureUtils
import monix.execution.atomic.Atomic
import toolbox6.akka.actor.PoolActor
import toolbox6.logging.LogTools

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal


/**
  * Created by martonpapp on 03/07/16.
  */
object JmsSink extends LazyLogging with LogTools {

  case class Conn(
    connection: Connection,
    dest: Destination,
    session: Session,
    producer: MessageProducer
  ) {
    def perform(send: Session => Message) : Unit = {
      producer.send(send(session))
    }

    def close = {
      quietly(producer.close())
      quietly(session.close())
      quietly(connection.close())
    }
  }

  type Connecter = () => Future[(ConnectionFactory, Destination)]


  def create[T](
    connecter: Connecter,
    sender: (T, Session) => Message
  )(implicit
    actorSystem: ActorRefFactory
  ) : Sink[T, Future[Unit]] = {
    Flow[T]
      .prefixAndTail(0)
      .flatMapConcat({
        case (_, tail) =>
          val pool =
            PoolActor
              .create(
                PoolActor.Config[Conn](

                )
              )



      })
      .toMat(Sink.ignore)(Keep.right)
  }

  def pool(
    connect : Connecter
  )(implicit
    executionContext: ExecutionContext
  ) : Pool = {
    new Pool {


      val pool = Atomic(List.empty[Conn])


      override def perform(send: (Session) => Message): Future[Unit] = Future {
        def retrieveConnection() = {
          pool.transformAndExtract({ list =>

            if (list.isEmpty) {
              val fut = for {
                (cf, dest) <- connect()
              } yield {
                val connection = cf.createConnection()
                val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
                val producer = session.createProducer(dest)

                val conn =
                  Conn(
                    connection,
                    dest,
                    session,
                    producer
                  )

                attempt(conn)
              }

              (fut, list)
            } else {
              (Future(attempt(list.head)), list.tail)
            }

          })

        }
        def attempt(retries : Int = 3) : Unit = {
          val conn = retrieveConnection()
          try {
            conn.perform(send)

            pool.transform(list => conn +: list)
          } catch {
            case NonFatal(_) =>
              conn.close

              if (retries > 0) attempt()
          }
        }




      }


      override def close: Unit = {
        pool.transform({ list =>
          list.foreach(_.close)
          List()
        })

      }
    }

  }

  def text(
    parallelism: Int,
    connect : Connecter
  )(implicit
    executionContext: ExecutionContext
  ) = apply[String](
    parallelism,
    connect,
    (msg, session) => {
      session.createTextMessage(msg)
    }
  )

  type TextHeaders = (String, Map[String, String])

  def textHeaders(
    parallelism: Int,
    connect : Connecter
  )(implicit
    executionContext: ExecutionContext
  ) : Sink[TextHeaders, Future[Unit]] = apply[TextHeaders](
    parallelism,
    connect,
    (msgHeader, session) => {
      val (msg, headers) = msgHeader
      val tm = session.createTextMessage(msg)
      headers.foreach({ case (k, v) =>
        tm.setStringProperty(k, v)
      })
      tm
    }
  )
  def apply[T](
    parallelism: Int,
    connect : Connecter,
    send: (T, Session) => Message
  )(implicit
    executionContext: ExecutionContext
  ) : Sink[T, Future[Unit]] = {
    Flow[T]
      .prefixAndTail(0)
      .flatMapConcat({
        case (Seq.empty, tail) =>
          val p = pool(connect)

          tail
            .mapAsync(parallelism)({ item =>
                p
                  .perform(session => send(item, session))

            })
            .fold()((_, _) => ())
            .map(
              Sink
                .ignore
                .mapMaterializedValue({ done =>
                  done
                    .andThen({ case _ => p.close })
                })
            )
      })
  }

}
