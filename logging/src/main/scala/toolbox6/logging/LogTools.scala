package toolbox6.logging

import com.typesafe.scalalogging.Logger

/**
  * Created by martonpapp on 01/10/16.
  */
trait LogTools {

  protected def logger : Logger

  def quietly( what: => Unit, msg: Option[String] = None ) : Unit = {
    try {
      what
    } catch {
      case ex : Throwable =>
        logger.warn(msg.getOrElse(ex.getMessage), ex)
    }
  }

}
