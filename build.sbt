enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

organization := "me.amanj"
name := "greenish"

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

// JS/JSX compiling

lazy val babel = taskKey[Seq[File]]("Compile JSX files")

babel in ThisBuild := Def.task {
  val src = (resourceDirectory in Compile).value / "dashboard"
  val destDir = (resourceManaged in Compile).value / "dashboard"
  destDir.mkdirs
  import scala.sys.process._
  src.listFiles.filter(_.getName.endsWith(".jsx")).map { file =>
    val dest = destDir / file.getName.dropRight(1)
    val compile =
      s"npx babel --minified --compact --out-file $dest --presets react-app/prod $file"
    val succeeded = Seq("bash", "-c", compile).!
    if(succeeded != 0) {
      throw new Exception("JSX Compilation failed")
    }
    dest
  }
}.value

resourceGenerators in Compile += babel.taskValue
babel := babel.triggeredBy(Compile / compile).value
