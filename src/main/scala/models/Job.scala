package me.amanj.greenish.models

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import io.circe.{Encoder, Decoder, HCursor, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}


case class Job(
  jobId: Int,
  name: String,
  cmd: String,
  timePattern: String,
  frequency: CheckFrequency,
  timezone: ZoneId,
  lookback: Int,
  alertLevels: AlertLevels,
) {
  val timeFormat = DateTimeFormatter.ofPattern(timePattern)
}

object Job {
  implicit val zoneIdEncoder: Encoder[ZoneId] =
    new Encoder[ZoneId] {
      final def apply(zid: ZoneId): Json = Json.obj(
        ("zoneId", Json.fromString(zid.getId))
      )
    }
  implicit val zoneIdDecoer: Decoder[ZoneId] = new Decoder[ZoneId] {
    final def apply(c: HCursor): Decoder.Result[ZoneId] =
      for {
        zoneId <- c.downField("zoneId").as[String]
      } yield ZoneId.of(zoneId)
  }

  implicit val checkEntryDecoder: Decoder[Job] = deriveDecoder
  implicit val checkEntryEncoder: Encoder[Job] = deriveEncoder
}
