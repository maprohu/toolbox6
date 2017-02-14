package toolbox6.akka.stream

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

/**
  * Created by pappmar on 14/02/2017.
  */
object Flows {

  def monitoring[T]() : Flow[T, T, Monitoring] = {
    Flow[T]
      .zipMat(
        Sources
          .singleMaterializedValue(() => new MonitoringImpl)
          .prefixAndTail(1)
          .flatMapConcat({
            case (Seq(monitoring), _) =>
              Source.repeat(monitoring)
          })
      )(Keep.right)
      .map({
        case (elem, mon) =>
          mon.onMessage()
          elem
      })


  }

}
