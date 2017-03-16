package toolbox6.akka.stream

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.Future


/**
  * Created by pappmar on 09/11/2016.
  */
object Sinks {

//  def materialized[In, Mat](
//    fn: () => (Sink[In, Future[Unit]], Mat)
//  )(implicit
//    materializer: Materializer
//  ) : Sink[In, (Mat, Future[Unit])] = {
//    import materializer.executionContext
//
//    Flow[In]
//      .prependMat(
//        Sources
//          .singleMaterializedValue(fn)
//          .mapMaterializedValue(_._2)
//      )(Keep.right)
//      .prefixAndTail(1)
//      .flatMapConcat({
//        case (Seq(sink), tail) =>
//          val complete = tail.asInstanceOf[Source[In, Any]]
//            .toMat(sink.asInstanceOf[Sink[In, Future[Unit]]])(Keep.right)
//            .run()
//
//          Source
//            .maybe
//            .mapMaterializedValue({ promise =>
//              promise
//                .tryCompleteWith(
//                  complete
//                    .map(_ => None)
//                )
//            })
//
//      })
//      .toMat(Sink.ignore)(Keep.both)
//  }



  def monitoring() : Sink[Any, Monitoring] = {
    Flow[Any]
      .zipMat(
        Sources
          .singleMaterializedValue(() => new MonitoringImpl)
          .prefixAndTail(1)
          .flatMapConcat({
            case (Seq(monitoring), _) =>
              Source.repeat(monitoring)
          })
      )(Keep.right)
      .to(
        Sink.foreach({
          case (_, mon) =>
            mon.onMessage()
        })
      )
  }

}

