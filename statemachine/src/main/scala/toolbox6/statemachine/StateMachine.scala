package toolbox6.statemachine

import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Future

/**
  * Created by pappmar on 14/10/2016.
  */


trait StateMachine[State, Event] {

  protected def initial : State

  private var current = initial

  protected def scheduler : Scheduler

  protected def transition(from: State, event: Event) : State

  def process(event: Event) : Unit = {
    scheduler.execute(new Runnable {
      override def run(): Unit = {
        current = transition(current, event)

      }
    })
  }




}
