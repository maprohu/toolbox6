package toolbox6.common

import java.io.{PrintWriter, StringWriter}

/**
  * Created by maprohu on 02-11-2016.
  */
object StringTools {

  def quietly(str: => String) = {
    try {
      str
    } catch {
      case ex: Throwable =>
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        ex.printStackTrace(pw)
        pw.flush()
        sw.flush()
        sw.toString
    }

  }

}
