import sbt._

object Dependencies {
  lazy val tapirVersion = "0.11.0"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
  lazy val tapirCore = "com.softwaremill.tapir" %% "tapir-core" % "0.11.0"
  lazy val tapirJsonCirce = "com.softwaremill.tapir" %% "tapir-json-circe" % "0.11.0"
  lazy val tapirOpenapiDocs = "com.softwaremill.tapir" %% "tapir-openapi-docs" % "0.11.0"
  lazy val tapirOpenapiModel = "com.softwaremill.tapir" %% "tapir-openapi-model" % "0.11.0"
  lazy val tapirOpenapiCirceYaml = "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml" % "0.11.0"
}
