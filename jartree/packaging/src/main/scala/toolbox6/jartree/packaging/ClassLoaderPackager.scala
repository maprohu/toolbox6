package toolbox6.jartree.packaging

import java.io.File

import mvnmod.builder.MavenTools.ProjectDef
import mvnmod.builder.{HasMavenCoordinates, MavenTools, Module, ModulePath}
import toolbox6.jartree.api.JarKey
import toolbox6.jartree.impl.JarTree

/**
  * Created by pappmar on 03/11/2016.
  */
object ClassLoaderPackager {
  val EmbeddedKeyProvider = JarTree.embeddedKey _

  case class Input(
    module: Module,
    target: ModulePath,
    keyProvider: HasMavenCoordinates => JarKey
  )

  def run(
    input: Input
  ) = {
    MavenTools
      .runMavens(
        process(input),
        Seq("install")
      )(_ => ())
  }


  def process(
    input: Input
  ) : ProjectDef = {
    import input._


    val coords =
      JarTree.metaModule(module)

    val pom =
      <project xmlns="http://maven.apache.org/POM/4.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        {coords.asPomCoordinates}
      </project>


    ProjectDef(
      coords,
      pom,
      preBuild = { file =>
        val resourceDir = new File(file, "src/main/resources")

        JarTree
          .writeMetaJarSeq(
            module,
            target,
            resourceDir,
            keyProvider
          )
      }
    )
  }

}
