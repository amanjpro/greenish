package me.amanj.greenish

import io.circe.generic.extras.Configuration
import io.circe.Json
import io.circe.syntax.EncoderOps

package object models {
  private[models] implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults
      .copy(transformConstructorNames = _.toLowerCase)

  def errorJson(str: String): Json = Json.obj (
    "error" -> str.asJson
  )
}
