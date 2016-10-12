package toolbox6.akka.json4s

import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.native.Serialization

import scala.concurrent.Future

/**
  * Created by pappmar on 12/10/2016.
  */
object AkkaJsonTools {

  trait Api extends Json4sSupport {
    import org.json4s._
    implicit val serialization = Serialization
    implicit val formats = Serialization.formats(NoTypeHints)
  }

  object Implicits {

    implicit class ResponseJsonOps(fr: Future[HttpResponse]) extends Api {

      def unpickle[T: Manifest](implicit
        materializer: Materializer
      ): Future[T] = {
        import materializer.executionContext
        for {
          r <- fr
          s <- Unmarshal(r).to[T]
        } yield {
          s
        }
      }

    }

  }

}
