package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class GroupStatus(
  group: Group,
  status: Seq[JobStatus],
)
object GroupStatus {
  implicit val groupStatusDecoder: Decoder[GroupStatus] = deriveConfiguredDecoder
  implicit val groupStatusEncoder: Encoder[GroupStatus] = deriveConfiguredEncoder
}
