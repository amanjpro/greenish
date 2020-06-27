package me.amanj.greenish.models

import java.time.ZonedDateTime
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

sealed trait CheckFrequency {
  def prev(date: ZonedDateTime): ZonedDateTime
}

object CheckFrequency {
  private implicit val config: Configuration =
    Configuration.default.copy(transformConstructorNames = _.toLowerCase)

  implicit val modeCodec: Codec[CheckFrequency] = deriveEnumerationCodec[CheckFrequency]
}

case object Hourly extends CheckFrequency {
  def prev(date: ZonedDateTime): ZonedDateTime =
    date
      .minusHours(1L)
}

case object Daily extends CheckFrequency {
  def prev(date: ZonedDateTime): ZonedDateTime =
    date
      .minusDays(1L)
}

case object Monthly extends CheckFrequency {
  def prev(date: ZonedDateTime): ZonedDateTime =
    date
      .minusMonths(1L)
}

case object Yearly extends CheckFrequency {
  def prev(date: ZonedDateTime): ZonedDateTime =
    date
      .minusYears(1L)
}
