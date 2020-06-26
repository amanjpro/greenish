package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class PeriodHealth (
  period: String,
  ok: Boolean,
)

object PeriodHealth {
  implicit val periodHealthDecoder: Decoder[PeriodHealth] = deriveDecoder
  implicit val periodHealthEncoder: Encoder[PeriodHealth] = deriveEncoder
}
