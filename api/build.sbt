import Dependencies._

ThisBuild / scalaVersion     := "2.13.0"
ThisBuild / organization     := "lavador"
ThisBuild / organizationName := "api"

lazy val root = (project in file("."))
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      tapirCore,
      tapirJsonCirce,
      tapirOpenapiDocs,
      tapirOpenapiCirceYaml,
      scalaTest % Test
    )
  )
