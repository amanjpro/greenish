package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class Lag(lag: Int)
object Lag {
  implicit val lagDecoder: Decoder[Lag] = deriveConfiguredDecoder
  implicit val lagEncoder: Encoder[Lag] = deriveConfiguredEncoder
}

