package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class AlertLevels(
  great: Int,
  normal: Int,
  warn: Int,
  critical: Int,
  )
object AlertLevels {
  implicit val alertLevelsDecoder: Decoder[AlertLevels] = deriveDecoder
  implicit val alertLevelsEncoder: Encoder[AlertLevels] = deriveEncoder
}
