package toolbox6.scalajs.server


import toolbox6.scalajs.server.RestServer.{Handler, HandlerResult}
import toolbox6.scalajs.server.Shared.{Request1, RequestBase}
import toolbox6.scalajs.shared.RestShared.{Config, Request}

import scala.concurrent.Future


/**
  * Created by pappmar on 25/10/2016.
  */

object Shared {


  sealed trait RequestBase {
    type Result
  }
  case class Request1(
    question: String
  ) extends RequestBase with Request[String]

}

object RunRestServer {



  def main(args: Array[String]): Unit = {

//    import upickle.default._
//    val handler = new Handler[RequestBase] {
//      override def handle[Res](req: RequestBase with Request[Res]): HandlerResult[Res] = {
//        import RestServer._
//
//        implicit class RequestOps[Res, Req <: Request[Res]](req: Req) {
//          def result(f: Future[Res])(implicit wr: Writer[Res]) : HandlerResult[Res] = {
//            HandlerResult(wr, f)
//          }
//        }
//
//        req match {
//          case r : Request1 =>
//            r.result(Future.successful(s"hello: ${r.question}"))
////            new RequestOps[String, Request1](r).result(Future.successful(s"hello: ${r.question}"))
//        }
//      }
//    }
//
//    RestServer
//      .serve(
//        Config(Seq("boo")),
//        handler
//      )
  }

}


