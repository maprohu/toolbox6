package toolbox6.docs

import scalatags.Text.all._
import scalatex.site.Main

/**
  * Created by pappmar on 13/10/2016.
  */
object DocsApi {

  implicit class FragHelper(val sc: StringContext) extends AnyVal {
    def d(args: Frag*): Frag = {
      sc
        .parts
        .map(s => s:Frag)
        .zip(args :+ (Seq[Frag]():Frag) )
        .flatMap(p => Seq(p._1, p._2))
    }
  }

  def munge(name: String): String = name.replace(" ", "")
}


trait DocsApi { self =>
  def ID : String
  def l(fn: self.type => String)(implicit main: Main) = {
    val name = fn(this)
    main.lnk(name, s"../${ID}/index.html#${DocsApi.munge(name)}")
  }


}