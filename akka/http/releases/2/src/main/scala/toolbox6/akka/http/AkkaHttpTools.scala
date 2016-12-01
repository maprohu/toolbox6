package toolbox6.akka.http

import akka.actor.FakeActorSystem
import akka.http.scaladsl.model.{HttpResponse, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.{PathMatchers, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import toolbox6.logging.LogTools

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Created by pappmar on 11/10/2016.
  */
object AkkaHttpTools extends LazyLogging with LogTools {

  object Implicits {

    implicit def segmentsToPathMatcher(segments: TraversableOnce[String]) : PathMatcher0 = {
      if (segments.isEmpty) {
        PathMatchers.Neutral
      } else {
        import akka.http.scaladsl.server.Directives._
        segments.map(segmentStringToPathMatcher).reduce(_ / _)
      }
    }

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

  def clearCache = {
    logger.info("clearing akka cache")
    quietly { (1 to 16).foreach { _ => RoutingSettings(FakeActorSystem()) } }
    logger.info("cleared akka cache")
  }

}
