package toolbox6.scalajs.client

import org.scalajs.dom
import toolbox6.scalajs.shared.RestShared._

import scala.concurrent.Future
import upickle.default._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by pappmar on 25/10/2016.
  */
object RestClient {

  def call[Response : Reader, Req <: Request[Response]: Writer](
    req: Req
  )(implicit
    config: Config
  ) : Future[Response] = {
    dom.ext.Ajax
      .post(
        url = config.path.mkString("/"),
        data = upickle.default.write(req)
      )
      .map({ o =>
        val env = upickle.default
          .read[ResponseEnvelope[Response]](o.responseText)

        env match {
          case ResponseSuccess(value) => value
          case ResponseFailure(message) => throw new Exception(message)
        }
      })
  }

}
