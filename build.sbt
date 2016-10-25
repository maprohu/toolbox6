name := "toolbox6-sbt"

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8"
)

lazy val shared =
  project
    .in(file("scalajs/shared"))
    .settings(commonSettings)
    .enablePlugins(ScalaJSPlugin)

lazy val client =
  project
    .in(file("scalajs/client"))
    .settings(commonSettings)
    .dependsOn(shared)
    .enablePlugins(ScalaJSPlugin)
    .settings(
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.1",
      libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.4.3"
    )
