package me.amanj.greenish.checker

import me.amanj.greenish.models.CheckEntry
import java.time.ZonedDateTime
import akka.actor.{Actor, ActorLogging}
import scala.sys.process._


case class EntryStatus (
  entry: CheckEntry,
  period: String,
  ok: Boolean,
)

case class EntryReport (
  entryRuns: Seq[EntryStatus]
)

case class Refresh(now: () => ZonedDateTime)
case object MaxLag
case object AllEntries
case object GetMissing


class StatusChecker(entries: List[CheckEntry],
    now: ZonedDateTime = ZonedDateTime.now()) extends Actor {
  private[this] var state = Seq.empty[EntryReport]
  refresh(now)

  def getMissing(): Seq[EntryStatus] = {
    state
      .map(_.entryRuns)
      .flatten
      .filter(!_.ok)
  }

  def maxLag(): Int = {
    state
      .map(_.entryRuns)
      .map { entryStatusList =>
        entryStatusList.filter(!_.ok).length
      }.max
  }

  def allEntries(): Seq[EntryReport] = state
  def getSummary(): Seq[EntryReport] =
    state

  private[this] def refresh(now: ZonedDateTime): Unit = {

    state = entries.map { entry =>
      var periods = Vector.empty[String]
      var nextStep = entry.frequency.jump(
        now.withZoneSameInstant(entry.timezone)
          .minusHours(entry.lookbackHours))

      while(! nextStep.isAfter(now)) {
        periods = periods :+ nextStep.format(entry.timeFormat)
        nextStep = entry.frequency.jump(nextStep)
      }
      (entry, periods)
    }.map { case (entry, periods) =>
      periods.map { period =>
        EntryStatus(entry, period, s"${entry.cmd} $period".! == 0)
      }
    }.map(EntryReport(_))
  }

  override def receive: Receive = {
    case Refresh(now) => refresh(now())
    case GetMissing => context.sender ! getMissing()
    case MaxLag => context.sender ! maxLag()
    case AllEntries => context.sender ! allEntries()
  }
}


