package toolbox6.scalajs.shared

/**
  * Created by pappmar on 25/10/2016.
  */
object RestShared {

  case class Config(
    path: Seq[String]
  )

//  trait Endpoint[Request, Response]

  trait Request[Response] {
    type Result = Response
  }

  sealed trait ResponseEnvelope[+Response]
  case class ResponseSuccess[+Response](
    value: Response
  ) extends ResponseEnvelope[Response]
  case class ResponseFailure(
    message: String
  ) extends ResponseEnvelope[Nothing]

}
