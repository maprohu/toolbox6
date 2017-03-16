package toolbox6.akka.stream

import monix.execution.atomic.Atomic
import scala.concurrent.duration._

case class MonitoringPeriod(
  start: Long,
  end: Long,
  count: Long
)

case class MonitoringStatus(
  at: Long,
  latest: Long,
  current: MonitoringPeriod,
  history: Iterable[MonitoringPeriod]
)

trait MonitoringInstance {
  def query:  MonitoringStatus
  def stop: Unit
}

case class CommonMonitoringStatus(
  perMinute: MonitoringStatus,
  perHour: MonitoringStatus
)

case class CommonMonitoring(
  perMinute: MonitoringInstance,
  perHour: MonitoringInstance
) {
  def query =
    CommonMonitoringStatus(
      perMinute = perMinute.query,
      perHour = perHour.query
    )
}

object Monitoring {
  val DefaultHistorySize = 10
}

trait Monitoring {
  def create(
    periodMillis: Long,
    historySize: Int = Monitoring.DefaultHistorySize
  ) : MonitoringInstance

  def createPerMinute(
    historySize: Int = Monitoring.DefaultHistorySize
  ) = create(1.minute.toMillis, historySize)

  def createPerHour(
    historySize: Int = Monitoring.DefaultHistorySize
  ) = create(1.hour.toMillis, historySize)

  def createCommon(
    historySize: Int = Monitoring.DefaultHistorySize
  ) = CommonMonitoring(
    createPerMinute(historySize),
    createPerHour(historySize)
  )
}

class MonitoringImpl extends Monitoring {

  val instances = Atomic(Vector.empty[MonitoringInstanceImpl])

  def onMessage() : Unit = {
    val ts = System.currentTimeMillis()
    instances.get.foreach(_.onMessage(ts))
  }

  override def create(periodMillis: Long, historySize: Int): MonitoringInstance = {
    val instance =
      new MonitoringInstanceImpl(periodMillis, historySize) {
        override def stop: Unit = {
          instances.transform(_.filterNot(_ == this))
        }
      }

    instances.transform({ is =>
      is :+ instance
    })

    instance
  }
}

abstract class MonitoringInstanceImpl(
  periodMillis: Long,
  historySize: Int
) extends MonitoringInstance {

  var latest : Long = 0
  var currentStart : Long = System.currentTimeMillis()
  var currentEnd : Long = currentStart + periodMillis
  var count : Long = 0
  var history = Vector.empty[MonitoringPeriod]

  def onMessage(ts: Long) : Unit = synchronized {
    latest = ts

    if (ts > currentEnd) {
      history =
        MonitoringPeriod(
          start = currentStart,
          end = currentEnd,
          count = count
        ) +:
        history
          .take(historySize - 1)

      val missingPeriodCount =
        (ts - currentEnd) / periodMillis

      currentStart = currentEnd + (periodMillis * missingPeriodCount)
      currentEnd = currentStart + periodMillis
      count = 0
    }

    count += 1
  }

  def query: MonitoringStatus = synchronized {
    MonitoringStatus(
      at = System.currentTimeMillis(),
      latest = latest,
      current = MonitoringPeriod(
        start = currentStart,
        end = currentEnd,
        count = count
      ),
      history = history
    )

  }

}

