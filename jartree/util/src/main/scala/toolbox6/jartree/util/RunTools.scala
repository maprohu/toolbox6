package toolbox6.jartree.util

import java.io.{ByteArrayOutputStream, PrintStream, PrintWriter}
import java.nio.charset.Charset

/**
  * Created by pappmar on 04/10/2016.
  */
object RunTools {

  val UTF8 = Charset.forName("UTF-8")

  def runBytes(what: => String) : Array[Byte] = {
    runByteArray {
      what.getBytes(UTF8)
    }
  }

  def runByteArray(what: => Array[Byte]) : Array[Byte] = {
    try {
      what
    } catch {
      case ex : Throwable =>
        val bs = new ByteArrayOutputStream()
        val pw = new PrintStream(bs, false, "UTF-8")
        try {
          ex.printStackTrace(pw)
          pw.flush()
          bs.flush()
          bs.toByteArray
        } finally {
          pw.close()
          bs.close()
        }
    }
  }

  def runBytesAll(
    what: (() => String)*
  ) : Array[Byte] = {
    what
      .toArray
      .flatMap({ w =>
        runBytes(w())
      })
  }

}
