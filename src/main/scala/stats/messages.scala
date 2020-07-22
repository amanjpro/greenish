package me.amanj.greenish.stats

import akka.http.scaladsl.model.HttpMethod

sealed trait Message
case class IncRefresh(job: String) extends Message
case class DecRefresh(job: String) extends Message
case class RefreshTime(job: String, duration: Double) extends Message
case class IncBadRefresh(job: String) extends Message
case class MissingPeriods(job: String, num: Int) extends Message
case class OldestMissingPeriod(job: String, num: Int) extends Message
case object GetPrometheus extends Message
case object GetStats extends Message
