package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class GroupStatusSummary(
  groupId: Int,
  name: String,
  status: Seq[JobStatusSummary],
)
object GroupStatusSummary {
  implicit val groupStatusSummaryDecoder: Decoder[GroupStatusSummary] = deriveConfiguredDecoder
  implicit val groupStatusSummaryEncoder: Encoder[GroupStatusSummary] = deriveConfiguredEncoder
}
