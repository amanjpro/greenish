package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class JobStatusSummary(
  jobId: Int,
  name: String,
  missing: Int,
  alertLevel: AlertLevel,
)
object JobStatusSummary {
  implicit val jobStatusSummaryDecoder: Decoder[JobStatusSummary] = deriveConfiguredDecoder
  implicit val jobStatusSummaryEncoder: Encoder[JobStatusSummary] = deriveConfiguredEncoder
}
