package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class JobStatusSummary(
  name: String,
  missing: Int,
  alertLevel: AlertLevel,
)
object JobStatusSummary {
  implicit val jobStatusSummaryDecoder: Decoder[JobStatusSummary] = deriveDecoder
  implicit val jobStatusSummaryEncoder: Encoder[JobStatusSummary] = deriveEncoder
}
