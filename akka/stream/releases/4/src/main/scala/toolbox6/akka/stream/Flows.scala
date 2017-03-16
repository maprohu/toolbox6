package toolbox6.akka.stream

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import scala.collection.immutable._

/**
  * Created by pappmar on 14/02/2017.
  */
object Flows {

  def materialized[T, Mat](
    mat: () => Mat
  ) : Flow[T, (T, Mat), Mat] = {
    Flow[T]
      .zipMat(
        Sources
          .singleMaterializedValue(mat)
          .prefixAndTail(1)
          .flatMapConcat({
            case (Seq(monitoring), _) =>
              Source.repeat(monitoring)
          })
      )(Keep.right)
  }

  def monitoring[T]() : Flow[T, T, Monitoring] = {
    materialized(() => new MonitoringImpl)
      .map({
        case (elem, mon) =>
          mon.onMessage()
          elem
      })
  }

  def historyMapConcat[In, Out, H <: AnyRef](
    fn: In => (Iterable[Out], Iterable[H]),
    capacity: Int = HistoryBuffer.DefaultCapacity
  ) : Flow[In, Out, HistoryBuffer[H]] = {
    materialized[In, HistoryBuffer[H]](() => new HistoryBuffer[H](capacity))
      .mapConcat({
        case (in, hb) =>
          val (out, hs) = fn(in)
          hs.foreach(hb.put)
          out
      })
  }

}
