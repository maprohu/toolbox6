package toolbox6.common

import java.util.concurrent.{SynchronousQueue, ThreadPoolExecutor, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.{Cancelable, Scheduler, UncaughtExceptionReporter}
import monix.execution.schedulers.{AsyncScheduler, ExecutionModel}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Promise}

/**
  * Created by martonpapp on 09/07/16.
  */
object HygienicThread extends LazyLogging {


  def execute[T]( task: => T, timeout: Duration = 10.minutes, classLoader: Option[ClassLoader] = None ) : T = {
    val promise = Promise[T]()
    val thread = new Thread() {
      override def run(): Unit = {
        try {
          promise.success(task)
        } catch {
          case ex : Throwable =>
            promise.failure(ex)
        }
      }
    }
    thread.setName("hygienic-" + Thread.currentThread().getName)
    classLoader.foreach({ cl =>
      thread.setContextClassLoader(cl)
    })
    thread.start()
    Await.result(promise.future, timeout)
  }

  def createExecutionContext(maxSize: Int = 32) : (ExecutionContext, () => Unit) = {
    logger.info("starting execution context")
    val pool = new ThreadPoolExecutor(
      0,
      maxSize,
      10L, TimeUnit.SECONDS,
      new SynchronousQueue[Runnable]
    )
    val ec = ExecutionContext.fromExecutor(pool)

    val shut = () => {
      logger.info("shutting down execution context")

      pool.shutdown()
      if (!pool.awaitTermination(15, TimeUnit.SECONDS)) {
        logger.warn("execution context not stopped, forcing")
        pool.shutdownNow()
        pool.awaitTermination(15, TimeUnit.SECONDS)
      }
      ()
    }

    (ec, shut)
  }

//  lazy private val (globalExecutionContext, globalStopper) = createExecutionContext()

  def createSchduler() : (Scheduler, () => Unit) = {
    val (ec, stopper) = createExecutionContext()
    val sch = AsyncScheduler(
      Scheduler.DefaultScheduledExecutor,
      ec,
      UncaughtExceptionReporter({ ex =>
        logger.warn(ex.getMessage, ex)
      }),
      ExecutionModel.Default
    )
    (sch, stopper)
  }

//  object Implicits extends LazyLogging {
//
//    implicit lazy val global: Scheduler =
//
//  }

//  def stopGlobal() = {
//    globalStopper()
//  }
//
//  val cancelGlobal = Cancelable(() => stopGlobal())

}

