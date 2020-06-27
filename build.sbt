enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

organization := "me.amanj"
name := "greenish"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.13.2"

val circeVersion = "0.13.0"
val akkaVersion = "2.6.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.12",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "io.circe" %% "circe-parser" % "0.13.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "com.typesafe" % "config" % "1.4.0",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.0" % Test,
)

dockerBaseImage := "openjdk:jre-alpine"

bashScriptExtraDefines += """addJava "-Dconfig.file=/app/config.yml""""
