package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class PeriodHealth (
  period: String,
  ok: Boolean,
)

object PeriodHealth {
  implicit val periodHealthDecoder: Decoder[PeriodHealth] = deriveConfiguredDecoder
  implicit val periodHealthEncoder: Encoder[PeriodHealth] = deriveConfiguredEncoder
}
