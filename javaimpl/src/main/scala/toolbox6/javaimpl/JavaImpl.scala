package toolbox6.javaimpl

import org.reactivestreams.{Processor, Publisher, Subscriber, Subscription}
import toolbox6.javaapi.{AsyncCallback, AsyncFunction, AsyncValue}

import scala.concurrent.{Future, Promise}

/**
  * Created by pappmar on 17/10/2016.
  */
object JavaImpl {

  def wrap[I, O](
    fn: AsyncFunction[I, O]
  )(
    input: I
  ) : Future[O] = {
    val promise = Promise[O]

    fn
      .apply(input)
      .onComplete(
        new AsyncCallback[O] {
          override def failure(throwable: Throwable): Unit = promise.failure(throwable)
          override def success(value: O): Unit = promise.success(value)
        }
      )

    promise.future
  }


  def processor[I, O](
    subscriber: Subscriber[I],
    publisher: Publisher[O]
  ) = new Processor[I, O] {
    override def onError(t: Throwable): Unit = subscriber.onError(t)
    override def onSubscribe(s: Subscription): Unit = subscriber.onSubscribe(s)
    override def onComplete(): Unit = subscriber.onComplete()
    override def onNext(t: I): Unit = subscriber.onNext(t)

    override def subscribe(s: Subscriber[_ >: O]): Unit = publisher.subscribe(s)
  }


  def asyncSuccess[O](value: O) = new AsyncValue[O] {
    override def onComplete(cb: AsyncCallback[O]): Unit = {
      cb.success(value)
    }
  }

}
