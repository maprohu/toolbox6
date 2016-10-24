package toolbox6.modules

import maven.modules.builder.{ScalaModule, SubModuleContainer}

/**
  * Created by pappmar on 30/08/2016.
  */
object GisModules {

  implicit val Container = SubModuleContainer(Toolbox6Modules.Root, "gis")

  object Util extends ScalaModule(
    "util",
    mvn.`org.orbisgis:h2spatial-ext:jar:1.2.4`,
    mvn.`commons-dbcp:commons-dbcp:jar:1.4`
  )

  object Geotools extends ScalaModule(
    "geotools",
    Util,
    Toolbox6Modules.Logging,
    mvn.`org.geotools:gt-opengis:jar:11.5`,
    mvn.`org.geotools:gt-main:jar:11.5`,
    mvn.`org.geotools:gt-coverage:jar:11.5`,
    mvn.`org.geotools:gt-referencing:jar:11.5`,
    mvn.`org.geotools:gt-api:jar:11.5`,
    mvn.`org.geotools:gt-render:jar:11.5`,
    mvn.`org.geotools:gt-epsg-wkt:jar:11.5`,
    mvn.`org.geotools:gt-jdbc:jar:11.5`,
    mvn.`org.geotools:gt-metadata:jar:11.5`
  )



}
