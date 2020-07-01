package me.amanj.greenish.checker

import java.time.ZonedDateTime
import me.amanj.greenish.models.JobStatus

sealed trait Message
case class Refresh(now: () => ZonedDateTime) extends Message
case object MaxLag extends Message
case object AllEntries extends Message
case object GetMissing extends Message
case object Summary extends Message
case class UpdateState(groupId: Int, newJobStatus: JobStatus) extends Message
case class GetJobStatus(groupId: Int, jobId: Int) extends Message
case class GetGroupStatus(groupId: Int) extends Message
case class Run(cmd: String, env: Seq[(String, String)]) extends Message
