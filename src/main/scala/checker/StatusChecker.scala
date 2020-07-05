package me.amanj.greenish.checker

import me.amanj.greenish.models._
import java.time.ZonedDateTime
import akka.actor.{Actor, Props, ActorLogging}
import scala.sys.process.Process
import scala.concurrent.{Future}
import scala.util.{Success, Failure}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.pattern.pipe
import scala.annotation.tailrec

trait StatusCheckerApi {
  protected[this] var state: IndexedSeq[GroupStatus]

  protected[checker] def getMissing(): IndexedSeq[GroupStatus] = {
    state
      .map { group =>
        val newJobs: Array[JobStatus] = group.status.map { job =>
          job.copy(periodHealth = job.periodHealth.filterNot(_.ok))
        }.filterNot(_.periodHealth.isEmpty)
          .toArray

        group.copy(status = newJobs)
      }.filterNot(_.status.isEmpty)
  }

  protected[checker] def maxLag(): Lag = {
    if(state.isEmpty) Lag(0)
    else {
      val lag = state.map { group =>
        group.status.map(_.countMissing).max
      }.max
      Lag(lag)
    }
  }

  protected[checker] def allEntries(): IndexedSeq[GroupStatus] = state

  protected[checker] def summary(): Seq[GroupStatusSummary] =
    state.map { group =>
      val status = group.status.map { status =>
        val missing = status.countMissing
        val alertLevel: AlertLevel =
          if(missing <= status.job.alertLevels.great) Great
          else if(missing <= status.job.alertLevels.normal) Normal
          else if(missing <= status.job.alertLevels.warn) Warn
          else Critical
        JobStatusSummary(status.job.name, missing, alertLevel)
      }.toSeq
      GroupStatusSummary(group.group.name, status)
    }

  protected[checker] def getGroupStatus(groupId: Int): Option[GroupStatus] =
    state.lift(groupId)

  protected[checker] def getJobStatus(groupId: Int, jobId: Int): Option[JobStatus] =
    for {
      group <- state.lift(groupId)
      job   <- group.status.lift(jobId)
    } yield job
}

class StatusChecker(groups: Seq[Group],
    env: Seq[(String, String)] = Seq.empty,
    clockCounter: () => Long = () => System.currentTimeMillis())
      extends Actor with ActorLogging with StatusCheckerApi {
  override protected[this] var state = StatusChecker.initState(groups)

  import context.dispatcher

  private[this] val parallelism: Int = groups.map(_.jobs.map(_.lookback).sum).sum

  private[this] val router = {
    val routees = (0 until parallelism) map { _ =>
      val runner = context.actorOf(
        Props(new CommandRunner()).withDispatcher("akka.refresh-dispatcher"))
      context watch runner
      ActorRefRoutee(runner)
    }

    Router(RoundRobinRoutingLogic(), routees)
  }

  private[this] def refresh(now: ZonedDateTime): Unit = {

    groups.foreach { group =>
      group.jobs.foreach { job =>
        val periods = StatusChecker.periods(job, now)

        val currentClockCounter = clockCounter()
        self ! BatchRun(job.cmd, periods, env,
          group.groupId, job.jobId, currentClockCounter)
      }
    }
  }

  override def receive: Receive = {
    case Refresh(now) =>
      refresh(now())
    case RunResult(periodHealth, groupId, jobId, clockCounter) =>
      val bucket = state(groupId)
      val currentStatus = bucket.status(jobId)
      if(currentStatus.updatedAt < clockCounter) {
        bucket.status(jobId) = currentStatus.copy(updatedAt = clockCounter,
          periodHealth = periodHealth)
      }
    case GetMissing => context.sender ! getMissing()
    case MaxLag => context.sender ! maxLag()
    case AllEntries => context.sender ! allEntries()
    case Summary => context.sender ! summary()
    case GetGroupStatus(id) =>
      context.sender ! getGroupStatus(id)
    case GetJobStatus(gid, jid) =>
      context.sender ! getJobStatus(gid, jid)
    case run: BatchRun =>
      router.route(run, context.sender)
  }
}

object StatusChecker {
  private[checker] def initState(groups: Seq[Group]): IndexedSeq[GroupStatus] = {
    groups.map { group =>
      val jobStatus = group.jobs.map { job =>
        JobStatus(job, -1, Seq.empty)
      }
      GroupStatus(group, jobStatus.toArray)
    }.toIndexedSeq
  }

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
