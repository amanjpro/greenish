package me.amanj.greenish.checker

import me.amanj.greenish.models.{CheckGroup, CheckEntry}
import java.time.ZonedDateTime
import akka.actor.{Actor, ActorLogging}
import io.circe._
import io.circe.generic.semiauto._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEnumerationCodec
import scala.sys.process._




case class PeriodHealth (
  period: String,
  ok: Boolean,
)
object PeriodHealth {
  implicit val periodHealthDecoder: Decoder[PeriodHealth] = deriveDecoder
  implicit val periodHealthEncoder: Encoder[PeriodHealth] = deriveEncoder
}

case class JobStatus (
  entry: CheckEntry,
  periodHealth: Seq[PeriodHealth],
) {
  def countMissing = periodHealth.count(!_.ok)
}
object JobStatus {
  implicit val jobStatusDecoder: Decoder[JobStatus] = deriveDecoder
  implicit val jobStatusEncoder: Encoder[JobStatus] = deriveEncoder
}

case class GroupStatus(
  group: CheckGroup,
  status: Seq[JobStatus],
) {
  def countMissing = status.map(_.countMissing).sum
}
object GroupStatus {
  implicit val groupStatusDecoder: Decoder[GroupStatus] = deriveDecoder
  implicit val groupStatusEncoder: Encoder[GroupStatus] = deriveEncoder
}

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

case class JobStatusSummary(
  name: String,
  missing: Int,
  alertLevel: AlertLevel,
)
object JobStatusSummary {
  implicit val jobStatusSummaryDecoder: Decoder[JobStatusSummary] = deriveDecoder
  implicit val jobStatusSummaryEncoder: Encoder[JobStatusSummary] = deriveEncoder
}
case class GroupStatusSummary(
  name: String,
  status: Seq[JobStatusSummary],
)
object GroupStatusSummary {
  implicit val groupStatusSummaryDecoder: Decoder[GroupStatusSummary] = deriveDecoder
  implicit val groupStatusSummaryEncoder: Encoder[GroupStatusSummary] = deriveEncoder
}

case class Refresh(now: () => ZonedDateTime)
case object MaxLag
case object AllEntries
case object GetMissing
case object Summary

class StatusChecker(groups: Seq[CheckGroup],
    now: ZonedDateTime = ZonedDateTime.now()) extends Actor {
  private[this] var state = Seq.empty[GroupStatus]
  refresh(now)

  def getMissing(): Seq[GroupStatus] = {
    state
      .map { group =>
        val newJobs: Seq[JobStatus] = group.status.map { job =>
          job.copy(periodHealth = job.periodHealth.filterNot(_.ok))
        }.filterNot(_.periodHealth.isEmpty)

        group.copy(status = newJobs)
      }.filterNot(_.status.isEmpty)
  }

  def maxLag(): Int = {
    state.map { group =>
        group.status.map(_.countMissing).max
      }.max
  }

  def allEntries(): Seq[GroupStatus] = state

  def summary(): Seq[GroupStatusSummary] =
    state.map { group =>
      val status = group.status.map { status =>
        val missing = status.countMissing
        val alertLevel: AlertLevel =
          if(missing <= status.entry.alertLevels.great) Great
          else if(missing <= status.entry.alertLevels.normal) Normal
          else if(missing <= status.entry.alertLevels.warn) Warn
          else Critical
        JobStatusSummary(status.entry.name, missing, alertLevel)
      }
      GroupStatusSummary(group.group.name, status)
    }

  private[this] def refresh(now: ZonedDateTime): Unit = {

    state = groups.map { group =>
      val jobStatusList = group.entries.map { entry =>
        var periods = Vector.empty[String]
        var nextStep = entry.frequency.jump(
          now.withZoneSameInstant(entry.timezone)
            .minusHours(entry.lookbackHours))

        while(! nextStep.isAfter(now)) {
          periods = periods :+ nextStep.format(entry.timeFormat)
          nextStep = entry.frequency.jump(nextStep)
        }

        val periodHealthList = periods.map { period =>
          PeriodHealth(period, s"${entry.cmd} $period".! == 0)
        }
        JobStatus(entry, periodHealthList)
      }
      GroupStatus(group, jobStatusList)
    }
  }


  override def receive: Receive = {
    case Refresh(now) => refresh(now())
    case GetMissing => context.sender ! getMissing()
    case MaxLag => context.sender ! maxLag()
    case AllEntries => context.sender ! allEntries()
    case Summary => context.sender ! summary()
  }
}


