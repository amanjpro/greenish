package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Group (
  groupId: Int,
  name: String,
  entries: Seq[Job],
)
object Group {
  implicit val checkGroupDecoder: Decoder[Group] = deriveDecoder
  implicit val checkGroupEncoder: Encoder[Group] = deriveEncoder
}
