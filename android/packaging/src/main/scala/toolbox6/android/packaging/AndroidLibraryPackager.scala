package toolbox6.android.packaging

import java.io.File

import org.jboss.shrinkwrap.resolver.api.maven.Maven

import scala.util.Properties
import ammonite.ops._

/**
  * Created by pappmar on 14/10/2016.
  */
object AndroidLibraryPackager {

  val AndroidHome = Properties.envOrNone("ANDROID_HOME").get


  def main(args: Array[String]): Unit = {

    implicit val wd = home
    val dx = Path(AndroidHome) / "build-tools" / "24.0.3" /  "dx.bat"

    println(dx.toIO)

    val target = pwd / up / "sandbox" / "target" / "out.dex"

    val file =
      Maven
        .resolver()
        .resolve(mvn.`org.scala-lang:scala-library:jar:2.11.8`.canonical)
        .withoutTransitivity()
        .asSingleFile()

    %(
      dx,
      "--dex",
      s"--output=${target}",
      file.getAbsolutePath

    )

//    val p = Runtime.getRuntime.exec(s"cmd /c ${dx.toIO.getAbsolutePath}")
//    p.waitFor()




    println(file)


  }

}
