package me.amanj.greenish.checker

import java.time.ZonedDateTime
import me.amanj.greenish.models.{JobStatus, PeriodHealth}

sealed trait Message
case class Refresh(now: () => ZonedDateTime) extends Message
case class RefreshGroup(now: () => ZonedDateTime, groupId: Int) extends Message
case class RefreshJob(now: () => ZonedDateTime, groupId: Int, jobId: Int) extends Message
case object MaxLag extends Message
case object AllEntries extends Message
case object GetMissing extends Message
case object Summary extends Message
case class GetJobStatus(groupId: Int, jobId: Int) extends Message
case class GetGroupStatus(groupId: Int) extends Message
case class BatchRun(cmd: String, periods: Seq[String], env: Seq[(String, String)],
  groupId: Int, jobId: Int, clockCounter: Long) extends Message
case class RunResult(periodHealth: Seq[PeriodHealth],
  groupId: Int, jobId: Int, clockCounter: Long) extends Message
