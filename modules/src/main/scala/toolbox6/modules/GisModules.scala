package toolbox6.modules

import mvnmod.builder.{Module, ScalaModule, SubModuleContainer}


/**
  * Created by pappmar on 30/08/2016.
  */
object GisModules {
  import mvnmod.builder.MavenCentralModule.Implicits._

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "gis")

  object Util extends ScalaModule(
    "util",
    mvn.`org.orbisgis:h2spatial-ext:jar:1.2.4`,
    mvn.`commons-dbcp:commons-dbcp:jar:1.4`
  ) {
    val Snapshot = snapshot
  }

  object Geotools extends ScalaModule(
    "geotools",
    Util.Snapshot,
    Toolbox6Modules.Logging.R2,
    mvn.`org.geotools:gt-opengis:jar:11.5`,
    mvn.`org.geotools:gt-main:jar:11.5`,
    mvn.`org.geotools:gt-referencing:jar:11.5`,
    mvn.`org.geotools:gt-api:jar:11.5`,
    mvn.`org.geotools:gt-render:jar:11.5`.exclude(mvn.`org.geotools:gt-coverage:jar:11.5`),
    mvn.`org.geotools:gt-coverage-patched:jar:11.5`,
    mvn.`org.geotools:gt-epsg-wkt:jar:11.5`,
    mvn.`org.geotools:gt-jdbc:jar:11.5`,
    mvn.`org.geotools:gt-metadata:jar:11.5`
  ) {
    val Snapshot = snapshot
  }



}
