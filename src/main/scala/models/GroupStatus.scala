package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class GroupStatus(
  group: Group,
  status: Seq[JobStatus],
) {
  def countMissing = status.map(_.countMissing).sum
}
object GroupStatus {
  implicit val groupStatusDecoder: Decoder[GroupStatus] = deriveDecoder
  implicit val groupStatusEncoder: Encoder[GroupStatus] = deriveEncoder
}