package me.amanj.greenish

import io.circe.generic.extras.Configuration
import io.circe.Json
import io.circe.syntax.EncoderOps
import java.lang.management.ManagementFactory

package object models {
  private[models] implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults
      .copy(transformConstructorNames = _.toLowerCase)

  def errorJson(str: String): Json = Json.obj (
    "error" -> str.asJson
  )

  def sysinfo(maybeNamespace: Option[String]): Json = {
    val maybeVersion = Option(getClass.getPackage.getImplementationVersion())
    Json.obj (
      "service" -> "Greenish".asJson,
      "namespace" -> maybeNamespace.asJson,
      "version" -> maybeVersion.asJson,
      "uptime" -> ManagementFactory.getRuntimeMXBean().getUptime().asJson,
    )
  }

  def okJson(str: String): Json = Json.obj (
    "ok" -> str.asJson
  )

  def healthJson(status: Boolean): Json = Json.obj (
    "health" -> (if(status) "good".asJson else "bad".asJson)
  )
}
