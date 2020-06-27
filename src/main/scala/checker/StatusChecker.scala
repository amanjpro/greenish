package me.amanj.greenish.checker

import me.amanj.greenish.models._
import java.time.ZonedDateTime
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.{Actor, Props, ActorLogging}
import scala.sys.process.Process
import scala.concurrent.{Future, Await}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.pattern.{pipe, ask}
import scala.language.postfixOps

class StatusChecker(groups: Seq[Group],
    env: Seq[(String, String)] = Seq.empty,
    now: ZonedDateTime = ZonedDateTime.now()) extends Actor with ActorLogging {
  private[this] var state = Seq.empty[GroupStatus]
  private[this] implicit val timeout = Timeout(2 minutes)

  import context.dispatcher

  private[this] val parallelism: Int = groups.map(_.entries.map(_.lookbackHours).sum).sum

  private[this] val router = {
    val routees = (0 until parallelism) map { _ =>
      val runner = context.actorOf(
        Props(new CommandRunner()).withDispatcher("akka.refresh-dispatcher"))
      context watch runner
      ActorRefRoutee(runner)
    }

    Router(RoundRobinRoutingLogic(), routees)
  }

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

  private[this] def refresh(now: ZonedDateTime): Seq[GroupStatus] = {

    val futureUpdate = groups.map { group =>
      val jobStatusListFutures = group.entries.map { entry =>
        val periods = StatusChecker.periods(entry, now)

        val periodHealthFutures = periods.map { period =>
          val cmd = s"${entry.cmd} $period"
          (period, self ? Run(cmd, env))
        }
        (entry, periodHealthFutures)
      }
      (group, jobStatusListFutures)
    }

    log.info("Refreshing the state")
    val r = futureUpdate.map { case (group, jobStatusListFutures) =>
      val jobStatusList = jobStatusListFutures.map { case (entry, periodHealthFutures) =>
        val periodHealthList = periodHealthFutures.map { case (period, futureHealth) =>
          PeriodHealth(period, Await.result(futureHealth.mapTo[Boolean], 2 minutes))
        }
        JobStatus(entry, periodHealthList)
      }
      GroupStatus(group, jobStatusList)
    }
    log.info("Refreshing done")
    r
  }

  override def receive: Receive = {
    case Refresh(now) =>
      val refreshFuture = Future {
        UpdateState(refresh(now()))
      }
      refreshFuture.pipeTo(self)
    case UpdateState(updated) => state = updated
    case GetMissing => context.sender ! getMissing()
    case MaxLag => context.sender ! maxLag()
    case AllEntries => context.sender ! allEntries()
    case Summary => context.sender ! summary()
    case run: Run =>
      router.route(run, context.sender)
  }
}

object StatusChecker {
  private[checker] def periods(entry: Job, now: ZonedDateTime): Seq[String]= {
    @tailrec def loop(time: ZonedDateTime, count: Int, acc: Seq[String]): Seq[String] = {
      if(count == 0) acc.reverse
      else
        loop(entry.frequency.prev(time), count - 1,
          acc :+ time.format(entry.timeFormat))
    }

    loop(now.withZoneSameInstant(entry.timezone),
      entry.lookback, Vector.empty[String])
  }
}
