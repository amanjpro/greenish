package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class AlertLevels(
  great: Int,
  normal: Int,
  warn: Int,
  critical: Int,
  )
object AlertLevels {
  implicit val alertLevelsDecoder: Decoder[AlertLevels] = deriveConfiguredDecoder
  implicit val alertLevelsEncoder: Encoder[AlertLevels] = deriveConfiguredEncoder
}
