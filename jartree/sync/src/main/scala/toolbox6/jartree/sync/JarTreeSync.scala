package toolbox6.jartree.sync

import java.io.File
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.StrictLogging
import toolbox6.logging.LogTools

/**
  * Created by pappmar on 30/11/2016.
  */
object JarTreeSync extends StrictLogging with LogTools {

  trait Processor {
    def close() : Unit
    def service(req: HttpServletRequest, resp: HttpServletResponse): Unit
  }

  class Holder(
    @volatile
  )

  case class Input(
    dir: File,
    init: () => Config
  )

  case class Config(
    versionMark: String
  )

  def run(
    input: Input,
    versionMarker: String
  ) = {

  }

  def runClean(
    input: Input
  ) = {


  }

}
