package me.amanj.greenish.alerts

import com.typesafe.config.Config
import me.amanj.greenish.models.{AlertLevel, Great, Normal, Warn, Critical}
import com.github.dikhan.pagerduty.client.events.PagerDutyEventsClient
import com.github.dikhan.pagerduty.client.events.domain.{Payload, Severity,
  TriggerIncident, ResolveIncident}
import java.time.OffsetDateTime

class PagerDutyClient(config: Config) extends AlertClient(config) {
  private[this] val eventApi = config.getString("event-api")

  private[this] val proxyHost = if(config.hasPath("proxy-host")) {
    Some(config.getString("proxy-host"))
  } else None

  private[this] val proxyPort = if(config.hasPath("proxy-port")) {
    Some(config.getInt("proxy-port"))
  } else None

  private[this] val doRetries = if(config.hasPath("do-retries")) {
    config.getBoolean("do-retries")
  } else false

  private[this] val routingKey = config.getString("routing-key")

  private[this] val client = {
    var tmp = new PagerDutyEventsClient.PagerDutyClientBuilder()
      .withEventApi(eventApi)
      .withDoRetries(doRetries)
    tmp = proxyHost.map { host => tmp.withProxyHost(host) }.getOrElse(tmp)
    tmp = proxyPort.map { port => tmp.withProxyPort(port) }.getOrElse(tmp)
    tmp.build()
  }

  def alert(job: JobInfo, level: AlertLevel): Unit = {
    val seveirtyLevel = level match {
      case Great | Normal => Severity.INFO
      case Warn => Severity.WARNING
      case Critical => Severity.CRITICAL
    }

    val payload = Payload.Builder.newBuilder()
      .setSummary(s"""|
        |${job.groupName}-${job.jobName} has problems, details are below:
        |
        |${job.stdout.mkString("\n")}
        |""".stripMargin)
      .setSource(System.getenv("HOSTNAME"))
      .setSeverity(seveirtyLevel)
      .setTimestamp(OffsetDateTime.now())
      .build()

    val incident = TriggerIncident.TriggerIncidentBuilder
      .newBuilder(routingKey, payload)
      .setDedupKey(job.id)
      .build()

    client.trigger(incident)
  }

  def resolve(job: JobInfo): Unit = {
    val resolve = ResolveIncident.ResolveIncidentBuilder
      .newBuilder(routingKey, job.id)
      .build();

    client.resolve(resolve);
  }
}
