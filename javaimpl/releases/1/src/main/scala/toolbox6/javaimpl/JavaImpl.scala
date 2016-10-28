package toolbox6.javaimpl

import org.reactivestreams.{Processor, Publisher, Subscriber, Subscription}
import toolbox6.javaapi.{AsyncCallback, AsyncFunction, AsyncValue}

import scala.concurrent.duration.Duration
import scala.concurrent._
import scala.util.{Failure, Success, Try}

/**
  * Created by pappmar on 17/10/2016.
  */
object JavaImpl {
  type ScalaCallback[O] = Try[O] => Unit

  def wrapCallback[O](
    cb: ScalaCallback[O]
  ) : AsyncCallback[O] = cb match {
    case cb : UnwrappedCallback[O] =>
      cb.cb
    case _ =>
      new WrappedCallback(cb)
  }

  def unwrapCallback[O](
    cb: AsyncCallback[O]
  ) : ScalaCallback[O] = cb match {
    case cb : WrappedCallback[O] =>
      cb.cb
    case _ =>
      new UnwrappedCallback(cb)
  }

  class UnwrappedCallback[O](
    val cb: AsyncCallback[O]
  ) extends ScalaCallback[O] {
    override def apply(value: Try[O]): Unit = value match {
      case Success(o) => cb.success(o)
      case Failure(ex) => cb.failure(ex)
    }
  }

  class WrappedCallback[O](
    val cb : ScalaCallback[O]
  ) extends AsyncCallback[O] {
    override def success(value: O): Unit = cb(Success(value))
    override def failure(throwable: Throwable): Unit = cb(Failure(throwable))
  }

  class WrappedFuture[O](
    val future: Future[O]
  )(implicit
    executionContext: ExecutionContext
  ) extends AsyncValue[O] {
    override def onComplete(cb: AsyncCallback[O]): Unit = future.onComplete(unwrapCallback(cb))
  }

  class UnwrappedFuture[O](
    val v: AsyncValue[O]
  ) extends Future[O] {
    val promise = Promise[O]()

    v.onComplete(
      wrapCallback[O]({ o => promise.complete(o) })
    )

    private def delegate = promise.future

    override def onComplete[U](f: (Try[O]) => U)(implicit executor: ExecutionContext): Unit = delegate.onComplete(f)

    override def isCompleted: Boolean = delegate.isCompleted

    override def ready(atMost: Duration)(implicit permit: CanAwait): UnwrappedFuture.this.type = {
      delegate.ready(atMost)
      this
    }

    override def result(atMost: Duration)(implicit permit: CanAwait): O = delegate.result(atMost)

    override def value: Option[Try[O]] = delegate.value
  }

  def unwrapFunction[I, O](
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

  def wrapFuture[O](
    future: Future[O]
  )(implicit
    executionContext: ExecutionContext
  ) : AsyncValue[O] = future match {
    case wf : UnwrappedFuture[O] =>
      wf.v
    case _ =>
      new WrappedFuture[O](future)
  }

  def unwrapFuture[O](
    value: AsyncValue[O]
  ) : Future[O] = value match {
    case wf : WrappedFuture[O] =>
      wf.future
    case _ =>
      new UnwrappedFuture[O](value)
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
