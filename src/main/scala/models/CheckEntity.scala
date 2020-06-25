package me.amanj.greenish.models

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.temporal.ChronoUnit

trait CheckFrequency {
  def jump(date: ZonedDateTime): ZonedDateTime
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

case class CheckGroup (
  groupId: Int,
  groupName: String,
  )

case class AlertLevels(
  great: Int,
  normal: Int,
  warn: Int,
  critical: Int,
  )

case class CheckEntry(
  jobId: Int,
  group: CheckGroup,
  name: String,
  cmd: String,
  timeFormat: DateTimeFormatter,
  frequency: CheckFrequency,
  timezone: ZoneId,
  lookbackHours: Int,
  alertLevels: AlertLevels,
)

