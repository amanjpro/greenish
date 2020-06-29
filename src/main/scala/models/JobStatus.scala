package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class JobStatus (
  job: Job,
  periodHealth: Seq[PeriodHealth],
) {
  def countMissing = periodHealth.count(!_.ok)
}
object JobStatus {
  implicit val jobStatusDecoder: Decoder[JobStatus] = deriveConfiguredDecoder
  implicit val jobStatusEncoder: Encoder[JobStatus] = deriveConfiguredEncoder
}
