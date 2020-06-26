package me.amanj.greenish.models

import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

sealed trait AlertLevel
object AlertLevel {
  private implicit val config: Configuration =
    Configuration.default.copy(transformConstructorNames = _.toLowerCase)

  implicit val modeCodec: Codec[AlertLevel] = deriveEnumerationCodec[AlertLevel]
}
case object Critical extends AlertLevel
case object Warn extends AlertLevel
case object Normal extends AlertLevel
case object Great extends AlertLevel
