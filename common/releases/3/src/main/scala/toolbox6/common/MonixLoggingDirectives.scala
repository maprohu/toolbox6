package toolbox6.common

import monix.execution.UncaughtExceptionReporter
import com.typesafe.scalalogging.Logger

/**
  * Created by pappmar on 11/10/2016.
  */
trait MonixLoggingDirectives {

  protected def logger : Logger

  val Reporter : monix.execution.UncaughtExceptionReporter = new UncaughtExceptionReporter {
    override def reportFailure(ex: Throwable): Unit = logger.error(ex.getMessage, ex)
  }

}
