enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

organization := "me.amanj"
name := "greenish"
version := "0.0.1-alpha-6"

scalaVersion := "2.13.2"

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-feature",
)

val circeVersion = "0.13.0"
val akkaVersion = "2.6.6"
val akkaHttpVersion = "10.1.12"
val typesafeConfigVersion = "1.4.0"
val scalaTestVersion = "3.2.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
)

dockerBaseImage := "openjdk:jre"

bashScriptExtraDefines += """addJava "-Dconfig.file=/app/config.yml""""
