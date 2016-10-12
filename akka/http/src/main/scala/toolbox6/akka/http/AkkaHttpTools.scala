package toolbox6.akka.http

import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Created by pappmar on 11/10/2016.
  */
object AkkaHttpTools {

  object Implicits {
    implicit class UriOps(uri: Uri) {
      def mapPath(fn: Path => Path): Uri = {
        uri.copy(path = fn(uri.path))
      }
    }

    implicit class ResponseOps(fr: Future[HttpResponse]) {
      def toStrict(implicit
        materializer: Materializer
      ) = {
        import materializer.executionContext
        for {
          r <- fr
          s <- r.entity.toStrict(10.minutes)
        } yield {
          r.copy(entity = s)
        }
      }

      def asString(implicit
        materializer: Materializer
      ) : Future[String] = {
        import materializer.executionContext
        for {
          r <- fr
          s <- Unmarshal(r).to[String]
        } yield {
          s
        }
      }


    }

  }

}
