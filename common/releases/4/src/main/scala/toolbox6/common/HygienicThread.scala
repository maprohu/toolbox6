package toolbox6.common

import java.lang.reflect.Field
import java.util.concurrent.{SynchronousQueue, ThreadPoolExecutor, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import monix.execution.{Cancelable, Scheduler, UncaughtExceptionReporter}
import monix.execution.schedulers.{AsyncScheduler, ExecutionModel}

import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.concurrent.{Await, ExecutionContext, Promise}
import scala.util.DynamicVariable

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



  val localRandom = {
    val localRandomField: Field =
      classOf[ThreadLocalRandom]
        .getDeclaredField("localRandom")
    localRandomField.setAccessible(true)
    localRandomField
      .get(null)
      .asInstanceOf[ThreadLocal[ThreadLocalRandom]]
  }

  def safe[T](what: => T) = {
    try {
      what
    } finally {
      listThreadLocals
      localRandom.remove()
    }

  }

  def listThreadLocals = {
    val thread = Thread.currentThread
    val threadLocalsField = classOf[Thread].getDeclaredField("threadLocals")
    val inheritableThreadLocalsField = classOf[Thread].getDeclaredField("inheritableThreadLocals")

    listThreadLocalsField(
      thread,
      threadLocalsField
    )
    listThreadLocalsField(
      thread,
      inheritableThreadLocalsField
    )
  }


  def listThreadLocalsField(
    thread: Thread,
    threadLocalsField: Field
  )= {
    threadLocalsField.setAccessible(true)
    val threadLocals = threadLocalsField.get(thread)

    if (threadLocals != null) {
      val threadLocalMapKlazz = Class.forName("java.lang.ThreadLocal$ThreadLocalMap")
      val tableField = threadLocalMapKlazz.getDeclaredField("table")
      tableField.setAccessible(true)

      val table = tableField.get(threadLocals)

      val threadLocalCount = java.lang.reflect.Array.getLength(table)

      var i: Int = 0
      while (i < threadLocalCount) {
        val entry = java.lang.reflect.Array.get(table, i)
        if (entry != null) {
          val valueField = entry.getClass.getDeclaredField("value")
          valueField.setAccessible(true)
          val value = valueField.get(entry)

          val referent =
            entry
              .asInstanceOf[java.lang.ref.WeakReference[ThreadLocal[_]]]
              .get()

          if (value != null) {
            //          logger.info(s"thread local: ${referent} -> ${value.getClass} - ${value}")

            if (referent == null) {
              logger.debug("cleaning field with null referent: {}", value)
              valueField.set(entry, null)
            } else if (referent.getClass.getClassLoader == classOf[DynamicVariable[_]].getClassLoader) {
              logger.debug("cleaning field with scala classloader: {} - {}", value.getClass, value)
              valueField.set(entry, null)
            }
          }
        }
        i += 1
      }

    }



  }

}

