enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

organization := "me.amanj"
name := "greenish"

scalaVersion := "2.13.2"
parallelExecution in ThisBuild := false

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
  "io.prometheus" % "simpleclient" % "0.9.0",
  "io.prometheus" % "simpleclient_common" % "0.9.0",
  "com.cronutils" % "cron-utils" % "9.1.0",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
)

dockerBaseImage := "openjdk:jre"

bashScriptExtraDefines += """addJava "-Dconfig.file=/app/config.yml""""

// JS/JSX compiling

lazy val jsxCompile = taskKey[Seq[File]]("Compile JSX files")

jsxCompile in ThisBuild := Def.task {
  val src = (resourceDirectory in Compile).value / "dashboard"
  val destDir = (resourceManaged in Compile).value / "dashboard"
  val srcFiles = src.listFiles.filter(_.getName.endsWith(".jsx")).toSet
  val stream = (jsxCompile / streams).value
  val JsxFilter: NameFilter =  "*.jsx"
  val JsFilter: NameFilter =  "*.js"
  destDir.mkdirs
  var errors = 0
  val cachedCompile =
    FileFunction.cached(stream.cacheDirectory / "jsx",
      FilesInfo.lastModified, FilesInfo.exists) { files =>
        stream.log.info("JSX compiler")
        import scala.sys.process._
        val compiled = srcFiles.map { file =>
          val dest = destDir / file.getName.dropRight(1)
          val compile =
            s"npx babel --minified --compact --out-file $dest --presets react-app/prod $file"
          val succeeded = Seq("bash", "-c", compile).!
          if(succeeded != 0) {
            errors += 1
          }
          dest
        }
        (compiled ** JsFilter).get.toSet
      }
  val files = cachedCompile((srcFiles ** JsxFilter).get.toSet).toSeq

  if(errors > 0) {
    throw new Error(s"JSX Compilation failed\nThere were $errors issues")
  }
  files
}.value

resourceGenerators in Compile += jsxCompile.taskValue
compile in Compile := (compile in Compile).dependsOn(jsxCompile).value
