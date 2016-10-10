package toolbox6.jms

import javax.jms.{ConnectionFactory, Destination, Message, Session}

import com.typesafe.scalalogging.{LazyLogging, StrictLogging}
import monix.execution.cancelables.SerialCancelable
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.{Observable, OverflowStrategy}
import toolbox6.logging.LogTools

import scala.concurrent.duration._

/**
  * Created by pappmar on 21/09/2016.
  */
object JmsObservable extends LazyLogging with LogTools {
  type JmsCoordinates = (ConnectionFactory, Destination)

  def create[E](
    endpoints: E*
  )(
    connecter: E => JmsCoordinates,
    stopper: SerialCancelable,
    bufferSize : Int = 1000,
    logDropped : Boolean = true
  )(implicit
    scheduler: Scheduler
  ) : Observable[Message] = {

    Observable
      .cons(
        endpoints,
        Observable
          .repeat(
            endpoints
          )
          .takeWhileNotCanceled(stopper)
          .delayOnNext(1.second)
      )
      .concatMap(Observable.fromIterable(_))
      .takeWhileNotCanceled(stopper)
      .flatMap({ endpoint =>
        Observable
          .create[Option[Message]](
            if (logDropped) {
              OverflowStrategy.DropOldAndSignal(
                bufferSize,
                { count =>
                  logger.debug(s"${count} messages dropped")
                  Some(None)
                }
              )
            } else {
              OverflowStrategy.DropOld(bufferSize)
            }
          )({ subscriber =>
            try {
              logger.info(s"Establishing connection to: ${endpoint}")

              val (connectionFactory, destination) = connecter(endpoint)

              val connection = connectionFactory.createConnection()
              val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
              val consumer = session.createConsumer(destination)
              connection.start()

              val receiveThread = new Thread {
                override def run(): Unit = {
                  try {
                    quietly {
                      while (
                        Option(consumer.receive())
                          .map({ msg =>
                            subscriber.onNext(Some(msg))
                          })
                          .isDefined
                      ) {}
                    }
                  } finally {
                    logger.info("jms thread exiting")
                    subscriber.onComplete()

                    quietly(consumer.close())
                    quietly(session.close())
                    quietly(connection.stop())
                    quietly(connection.close())

                    logger.info("jms thread exit complete")
                  }
                }
              }
              receiveThread.setName("JMSObservable-poll-thread")
              receiveThread.start()

              val cancel =
                Cancelable({ () =>
                  quietly(consumer.close())
                })

              logger.info("Connection established")

              stopper := cancel

              cancel
            } catch {
              case ex : Throwable =>
                logger.warn("Connection failed", ex)
                subscriber.onComplete()
                Cancelable.empty
            }
          })
          .collect({
            case Some(msg) => msg
          })
      })



  }


  trait Implicits {
    implicit class Ops(c: JmsCoordinates) {
      def observable(implicit scheduler: Scheduler) = JmsObservable.create(c)(identity, SerialCancelable())
    }
  }
  object Implicits extends Implicits

}
