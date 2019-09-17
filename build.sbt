val commonSettings = Seq(
  organization := "lavador",
  scalaVersion := "2.13.0",
  version := "0.0.1",
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0"),
)

val tapirVersion = "0.11.0"
val Http4sVersion = "0.21.0-M4"
val CirceVersion = "0.12.0-M4"
val Specs2Version = "4.7.0"
val LogbackVersion = "1.2.3"


lazy val rootProject = (project in file("."))
  .settings(commonSettings)
  .settings(name := "lavador")
  .aggregate(
    api,
    mixer,
    jobcoin,
  )

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"
//lazy val tapirOpenapiModel = "com.softwaremill.tapir" %% "tapir-openapi-model" % tapirVersion
lazy val api = project
  .settings(commonSettings)
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      "com.softwaremill.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
      scalaTest % Test
    )
  )

lazy val mixer = project
  .settings(commonSettings)
  .settings(
    name := "mixer",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "com.softwaremill.tapir" %% "tapir-http4s-server" % tapirVersion,
      scalaTest % "test",
    )

  ).dependsOn(api)

lazy val jobcoin = project
  .settings(commonSettings)
  .settings(
    name := "jobcoin",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "com.softwaremill.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.tapir" %% "tapir-swagger-ui-http4s" % tapirVersion,
      scalaTest % "test",
    )
  ).dependsOn(api)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Xfatal-warnings",
  )
