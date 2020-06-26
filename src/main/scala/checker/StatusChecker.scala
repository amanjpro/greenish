package me.amanj.greenish.checker

import me.amanj.greenish.models._
import java.time.ZonedDateTime
import akka.actor.{Actor, ActorLogging}
import scala.sys.process._

class StatusChecker(groups: Seq[Group],
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


