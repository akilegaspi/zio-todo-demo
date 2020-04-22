ThisBuild / scalaVersion := "2.12.11"
ThisBuild / organization := "com.akilegaspi"

lazy val zioDemo = (project in file("."))
  .settings(
    name := "ZIO Demo",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.0-RC18-2",
      "org.xerial" % "sqlite-jdbc" % "3.30.1"
    ),
    scalacOptions += "-Ypartial-unification"
  )
