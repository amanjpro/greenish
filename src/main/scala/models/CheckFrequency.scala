package me.amanj.greenish.models

import java.time.ZonedDateTime
import com.cronutils.model.time.ExecutionTime
import com.cronutils.model.CronType.UNIX
import com.cronutils.parser.CronParser
import com.cronutils.model.definition.CronDefinitionBuilder
import io.circe.{Printer, Decoder, Encoder, HCursor, Json}
import io.circe.syntax.EncoderOps
import io.circe.generic.extras.semiauto.{
  deriveEnumerationCodec, deriveConfiguredDecoder, deriveConfiguredEncoder}

sealed trait CheckFrequency {
  def prev(date: ZonedDateTime): ZonedDateTime
}

object CheckFrequency {
  implicit val freqDecoder: Decoder[CheckFrequency] = new Decoder[CheckFrequency] {
    final def apply(obj: HCursor): Decoder.Result[CheckFrequency] = {
      obj.as[String].map {
        case "hourly"   => Right(Hourly)
        case "daily"    => Right(Daily)
        case "monthly"  => Right(Monthly)
        case "annually" => Right(Annually)
      }.getOrElse(obj.as[Cron])
    }
  }

  implicit val freqEncoder: Encoder[CheckFrequency] = Encoder.instance {
    case Hourly         => "hourly".asJson
    case Daily          => "daily".asJson
    case Monthly        => "monthly".asJson
    case Annually       => "annually".asJson
    case other: Cron    => other.asJson
  }
}

case class Cron(pattern: String) extends CheckFrequency {
  private[this] val parser = new CronParser(
    CronDefinitionBuilder.instanceDefinitionFor(UNIX))
  private[this] val executionTime = ExecutionTime.forCron(
    parser.parse(pattern))

  def prev(date: ZonedDateTime): ZonedDateTime =
    executionTime.lastExecution(date).get()
}
object Cron {
  implicit val cronDecoder: Decoder[Cron] = deriveConfiguredDecoder
  implicit val checkGroupEncoder: Encoder[Cron] = deriveConfiguredEncoder
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

case object Annually extends CheckFrequency {
  def prev(date: ZonedDateTime): ZonedDateTime =
    date
      .minusYears(1L)
}
