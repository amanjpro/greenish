package me.amanj.greenish.models

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEnumerationCodec


sealed trait CheckFrequency {
  def jump(date: ZonedDateTime): ZonedDateTime
}
object CheckFrequency {
  private implicit val config: Configuration =
    Configuration.default.copy(transformConstructorNames = _.toLowerCase)

  implicit val modeCodec: Codec[CheckFrequency] = deriveEnumerationCodec[CheckFrequency]
}

case object Hourly extends CheckFrequency {
  def jump(date: ZonedDateTime): ZonedDateTime =
    date
      .plusHours(1L)
}

case object Daily extends CheckFrequency {
  def jump(date: ZonedDateTime): ZonedDateTime =
    date
      .plusDays(1L)
}
//
// case object Weekly extends CheckFrequency {
//   def jump(date: ZonedDateTime): ZonedDateTime =
//     date
//       .plusWeeks(1L)
// }

case object Monthly extends CheckFrequency {
  def jump(date: ZonedDateTime): ZonedDateTime =
    date
      .plusMonths(1L)
}

case object Yearly extends CheckFrequency {
  def jump(date: ZonedDateTime): ZonedDateTime =
    date
      .plusYears(1L)
}

case class AlertLevels(
  great: Int,
  normal: Int,
  warn: Int,
  critical: Int,
  )
object AlertLevels {
  implicit val alertLevelsDecoder: Decoder[AlertLevels] = deriveDecoder
  implicit val alertLevelsEncoder: Encoder[AlertLevels] = deriveEncoder
}

case class CheckEntry(
  jobId: Int,
  name: String,
  cmd: String,
  timePattern: String,
  frequency: CheckFrequency,
  timezone: ZoneId,
  lookbackHours: Int,
  alertLevels: AlertLevels,
) {
  val timeFormat = DateTimeFormatter.ofPattern(timePattern)
}

object CheckEntry {
  implicit val zoneIdEncoder: Encoder[ZoneId] =
    new Encoder[ZoneId] {
      final def apply(zid: ZoneId): Json = Json.obj(
        ("zone-id", Json.fromString(zid.getId))
      )
    }
  implicit val zoneIdDecoer: Decoder[ZoneId] = new Decoder[ZoneId] {
    final def apply(c: HCursor): Decoder.Result[ZoneId] =
      for {
        zoneId <- c.downField("zone-id").as[String]
      } yield ZoneId.of(zoneId)
  }

  implicit val checkEntryDecoder: Decoder[CheckEntry] = deriveDecoder
  implicit val checkEntryEncoder: Encoder[CheckEntry] = deriveEncoder
}

case class CheckGroup (
  groupId: Int,
  name: String,
  entries: Seq[CheckEntry],
)
object CheckGroup {
  implicit val checkGroupDecoder: Decoder[CheckGroup] = deriveDecoder
  implicit val checkGroupEncoder: Encoder[CheckGroup] = deriveEncoder
}


