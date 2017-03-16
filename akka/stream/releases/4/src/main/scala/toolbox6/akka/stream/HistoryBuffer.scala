package toolbox6.akka.stream

import scala.collection.immutable.Iterable

object HistoryBuffer {
  val DefaultCapacity = 25
}

case class CommonHistoryItem(
  timestamp: Long,
  message: Any,
  exception: Throwable
)

class HistoryBuffer[T <: AnyRef](
  capacity: Int = HistoryBuffer.DefaultCapacity
) {
  private val buffer = Array.ofDim[AnyRef](capacity)
  private var full = false
  private var next = 0

  def put(elem: T) = synchronized {
    buffer(next) = elem
    next += 1
    if (next >= capacity) {
      next = 0
      full = true
    }
  }

  def content : Iterable[T] = synchronized {

    if (full) {
      val (left, right) =
        buffer
          .view
          .splitAt(next)

      right
        .++(left)
        .toVector
        .asInstanceOf[Iterable[T]]
    } else {
      buffer
        .view
        .take(next)
        .toVector
        .asInstanceOf[Iterable[T]]
    }

  }

}

