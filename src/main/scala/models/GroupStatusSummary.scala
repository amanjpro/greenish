package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class GroupStatusSummary(
  name: String,
  status: Seq[JobStatusSummary],
)
object GroupStatusSummary {
  implicit val groupStatusSummaryDecoder: Decoder[GroupStatusSummary] = deriveDecoder
  implicit val groupStatusSummaryEncoder: Encoder[GroupStatusSummary] = deriveEncoder
}
