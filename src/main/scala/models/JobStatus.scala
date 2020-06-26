package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class JobStatus (
  entry: Job,
  periodHealth: Seq[PeriodHealth],
) {
  def countMissing = periodHealth.count(!_.ok)
}
object JobStatus {
  implicit val jobStatusDecoder: Decoder[JobStatus] = deriveDecoder
  implicit val jobStatusEncoder: Encoder[JobStatus] = deriveEncoder
}
