package me.amanj.greenish.alerts

import com.typesafe.config.Config
import me.amanj.greenish.models.AlertLevel

case class JobInfo(id: String,
  groupName: String, jobName: String, stdout: Seq[String])

abstract class AlertClient(config: Config) {
  def alert(job: JobInfo, level: AlertLevel): Unit

  def resolve(job: JobInfo): Unit
}
