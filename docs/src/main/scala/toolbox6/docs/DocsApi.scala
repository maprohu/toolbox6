package toolbox6.docs

import scalatags.Text.all._
import scalatags.Text.{Attrs => _, Frag => _, Styles => _, _}
import scalatags.{DataConverters, text}
import scalatex.site.Main

/**
  * Created by pappmar on 13/10/2016.
  */
object DocsApi {


}

class DocString(val str: String) {
  import scalatags.Text.all._
  def term = i(str)
}
object DocString {
  implicit def toFrag(ds: DocString) : Frag = ds.str
}

trait DocsApi
  extends Cap
    with Attrs
    with Styles
    with text.Tags
    with DataConverters
    with Aggregate { self =>



  def ID : String

  def htmlFileName = {
     s"${ID}.html"
  }

  def l(fn: self.type => String)(implicit main: Main) = {
    val name = fn(this)
//    main.lnk(name, s"../${ID}/index.html#${munge(name)}")
    main.lnk(name, s"${htmlFileName}#${munge(name)}")
  }

  implicit class FragHelper(val sc: StringContext) {
    def d(args: Frag*): Frag = {
      sc
        .parts
        .map(s => s:Frag)
        .zip(args :+ (Seq[Frag]():Frag) )
        .flatMap(p => Seq(p._1, p._2))
    }
  }

  def munge(name: String): String = name.replace(" ", "")

  case class Ref(
    text: String
  )

  implicit class DocsStringOps(str: String) {
    def ref : Ref = Ref(str)
    def doc : DocString = new DocString(str)
    def link = {
      a(href := str)(str)
    }
    def local = {
      a(href := s"#$str")(str)
    }
  }

}