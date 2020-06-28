package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Lag(lag: Int)
object Lag {
  implicit val lagDecoder: Decoder[Lag] = deriveDecoder
  implicit val lagEncoder: Encoder[Lag] = deriveEncoder
}

