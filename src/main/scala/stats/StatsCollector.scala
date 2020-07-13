package me.amanj.greenish.stats

import me.amanj.greenish.models._
import java.time.ZonedDateTime
import akka.actor.Actor
import scala.sys.process.Process
import scala.util.control.NonFatal
import akka.actor.ActorLogging
import io.prometheus.client.{Counter, Gauge}
import akka.http.scaladsl.model.HttpMethod
import me.amanj.greenish.endpoints.Routes

case class IncRequest(method: HttpMethod)
case class IncBadResponse(method: String)
case class IncRefresh(job: String)
case class DecRefresh(job: String)

class StatsCollector(jobIDs: Set[String]) extends Actor with ActorLogging {

  val requestsCounter = Counter.build()
    .name("greenish_requests_total")
    .help("Total number of HTTP requests")
    .labelNames("http_method")
    .register()

  val badResponsesCounter = Counter.build()
    .name("greenish_bad_responses_total")
    .help("Total number of non-OK HTTP reposnoses")
    .labelNames("http_method")
    .register()

  val refreshGauge = Gauge.build()
    .name("greenish_active_refresh_tasks")
    .help("Current number active state refresh tasks")
    .labelNames("job_id")
    .register()

  val responseTime = Counter.build()
    .name("greenish_response_time_seconds")
    .help("HTTP Response time")
    .labelNames("endpoint")
    .register()

  val refreshTime = Counter.build()
    .name("greenish_state_refresh_time_seconds")
    .help("Job state refreshing time")
    .labelNames("job_id")
    .register()

  val refreshCounter = Counter.build()
    .name("greenish_state_refresh_total")
    .help("Total number of job state refresh instances")
    .labelNames("job_id")
    .register()

   val badRefreshCounter = Counter.build()
    .name("greenish_state_refresh_failed_total")
    .help("Total number of failed job state refresh instances")
    .labelNames("job_id")
    .register()

  val missingPeriods = Gauge.build()
    .name("greenish_missing_periods_total")
    .help("Current number of missing dataset periods")
    .labelNames("job_id")
    .register()

  def init(): Unit = {
    import StatsCollector.AllMethods
    AllMethods.foreach(requestsCounter.labels(_))
    AllMethods.foreach(badResponsesCounter.labels(_))
    jobIDs.foreach(refreshGauge.labels(_))
    jobIDs.foreach(missingPeriods.labels(_))
    jobIDs.foreach(refreshTime.labels(_))
  }
  override def receive: Receive = {
    case IncRequest(method) =>
      requestsCounter.labels(method.name.toLowerCase).inc()
      requestsCounter.labels().get
  }
}

object StatsCollector {
  val AllMethods = Set(
    "connect",
    "delete",
    "get",
    "head",
    "options",
    "patch",
    "post",
    "put",
    "trace",
    )
}
