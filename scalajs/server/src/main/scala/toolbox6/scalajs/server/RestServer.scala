package toolbox6.scalajs.server



import akka.http.scaladsl.server.Route
import toolbox6.scalajs.shared.RestShared._
import upickle.default._

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * Created by pappmar on 25/10/2016.
  */
object RestServer {

  import akka.http.scaladsl.server.Directives._

//  def handler[Res : Writer, Req <: Request[Res] : ClassTag](
//    fn: Req => Future[Res]
//  ) : (Class[Req], Any => String) = {
//    val cl = classOf[Req]
//
//    implicit val writer = implicitly[Writer[Res]]
//    val fnr = { (req:Req) =>
//      fn(req)
//        .map(r => write(r))
//
//    }
//
//  }

  case class HandlerResult[Res](
    writer: Writer[Res],
    result: Future[Res]
  )

  object HandlerResult {
    implicit def wrap[Res : Writer](res: Future[Res]) = {
      HandlerResult[Res](
        implicitly[Writer[Res]],
        res
      )
    }

//    def apply[T <: Request](
//      f: Future[T.Result]
//    )(implicit wr: Writer[Res])
  }

  trait Handler[Req] {
    def handle[Res](
      req: Req with Request[Res]
    ) : HandlerResult[Res]
  }

  def serve[ReqBase : Reader](
    config: Config,
    handler: Handler[ReqBase]
  ) : Route = {
    path(
      config.path.foldLeft(Neutral)(_ / _)
    ) {
      entity(as[String]) { entity =>

        def wr[T](hr: HandlerResult[T]) = {
          implicit val writer = hr.writer
          onComplete(hr.result) { result =>
            complete(
              result
                .map({ r =>
                  write(
                    ResponseSuccess(r)
                  )
                })
                .recover({
                  case ex =>
                    write(
                      ResponseFailure(ex.getMessage)
                    )
                })
                .get
            )
          }
        }

        wr(
          handler
            .handle(
              read[ReqBase](entity)
                .asInstanceOf[ReqBase with Request[Any]]
            )
        )



      }
    }

  }

}
