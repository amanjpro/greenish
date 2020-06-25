organization := "me.amanj"
name := "greenish"
version := "0.0.1"

scalaVersion := "2.13.2"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.12",
  "com.typesafe.akka" %% "akka-stream" % "2.6.6",
  "io.circe" %% "circe-parser" % "0.13.0",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "com.typesafe" % "config" % "1.4.0",
)

