package toolbox6.modules

import java.io.File

import maven.modules.builder.MavenTools

/**
  * Created by pappmar on 05/10/2016.
  */
object RunInstallToolbox6Modules {

  def main(args: Array[String]): Unit = {

    val root = new File("../../../../..")

    MavenTools.runMaven(
      MavenTools.pom {
        <packaging>pom</packaging>
        <modules>
          <module>{root}/maven-modules</module>
          <module>{root}/toolbox6/modules</module>
          <module>{root}/toolbox6</module>
        </modules>
      },
      "install"
    )(_ => ())


  }

}
