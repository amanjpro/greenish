package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class Group (
  groupId: Int,
  name: String,
  entries: Seq[Job],
)
object Group {
  implicit val checkGroupDecoder: Decoder[Group] = deriveConfiguredDecoder
  implicit val checkGroupEncoder: Encoder[Group] = deriveConfiguredEncoder
}
