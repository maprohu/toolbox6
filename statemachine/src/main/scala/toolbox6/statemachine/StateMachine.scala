package toolbox6.statemachine

import monix.eval.Task
import monix.reactive.Observable


/**
  * Created by pappmar on 14/10/2016.
  */
case class State[I, O](
  out: Observable[O] = Observable.empty,
  fn: I => State[I, O]
) {
  def transformer : Observable[I] => Observable[O] = { o =>
    Observable
      .concat(
        out,
        o
          .scan(
            this
          )({ (state, in) =>
            state.fn(in)
          })
          .flatMap(_.out)
      )
  }
}

object State {
  def end[I, O](out: Observable[O]) : State[I, O] = State[I, O](
    out = out,
    fn = _ => end(Observable.empty)
  )

}

case class StateAsync[I, O](
  out: Observable[O] = Observable.empty,
  fn: I => Task[StateAsync[I, O]]
) {
  def transformer : Observable[I] => Observable[O] = { o =>
    Observable
      .concat(
        out,
        o
          .flatScan(
            this
          )({ (state, in) =>
            Observable
              .fromTask(
                state.fn(in)
              )
          })
          .flatMap(_.out)
      )
  }
}

object StateAsync {
  def end[I, O](out: Observable[O] = Observable.empty) : StateAsync[I, O] = {
    lazy val End : StateAsync[I, O] = StateAsync[I, O](
      fn = _ => Task.now(End)
    )

    StateAsync[I, O](
      out = out,
      fn = _ => Task.now(End)
    )
  }
}
