package me.amanj.greenish.stats

import akka.actor.Actor
import akka.actor.ActorLogging
import io.prometheus.client.{Counter, Gauge, Histogram, CollectorRegistry}
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.exporter.common.TextFormat
import java.util.Enumeration
import java.io.{StringWriter, Writer}
import akka.http.scaladsl.model.{MediaType, HttpCharsets, HttpEntity}
import akka.http.scaladsl.marshalling.{ToEntityMarshaller, Marshaller}

class StatsCollector(jobIDs: Set[String],
    registry: CollectorRegistry = new CollectorRegistry()) extends Actor with ActorLogging {

  // Job related metrics
  val refreshGauge = Gauge.build()
    .name("greenish_active_refresh_tasks")
    .help("Current number active state refresh tasks")
    .labelNames("job_id")
    .register(registry)

  val refreshTime = Histogram.build()
    .name("greenish_state_refresh_time_seconds")
    .help("Job state refreshing time")
    .labelNames("job_id")
    .buckets(StatsCollector.HistogramTimeBuckets:_*)
    .register(registry)

  val refreshCounter = Counter.build()
    .name("greenish_state_refresh_total")
    .help("Total number of job state refresh instances")
    .labelNames("job_id")
    .register(registry)

   val badRefreshCounter = Counter.build()
    .name("greenish_state_refresh_failed_total")
    .help("Total number of failed job state refresh instances")
    .labelNames("job_id")
    .register(registry)

  val missingPeriods = Gauge.build()
    .name("greenish_missing_periods_total")
    .help("Current number of missing dataset periods")
    .labelNames("job_id")
    .register(registry)

  def init(): Unit = {
    jobIDs.foreach { jobId =>
      refreshGauge.labels(jobId)
      refreshTime.labels(jobId)
      refreshCounter.labels(jobId)
      badRefreshCounter.labels(jobId)
      missingPeriods.labels(jobId)
    }
  }
  override def receive: Receive = {
    case RefreshTime(jobId, time) =>
      refreshTime.labels(jobId).observe(time)
    case IncRefresh(jobId) =>
      refreshCounter.labels(jobId).inc()
      refreshGauge.labels(jobId).inc()
    case DecRefresh(jobId) =>
      refreshGauge.labels(jobId).dec()
    case IncBadRefresh(jobId) =>
      badRefreshCounter.labels(jobId).inc()
    case MissingPeriods(jobId, count) =>
      missingPeriods.labels(jobId).set(count)
    case GetPrometheus =>
      import StatsCollector.{fromRegistry, toPrometheusTextFormat}
      val metrics = fromRegistry(registry)
      context.sender ! metrics
  }
}

object StatsCollector {
  case class MetricsEntity(samples: Enumeration[MetricFamilySamples])

  private [StatsCollector] val HistogramTimeBuckets =
    Seq(
      0.1, 0.3, 0.5, 0.8, 1, 1.3, 1.5, 1.8, 2, 2.5, 3, 3.5, 4, 4.5)

  private[this] val mediaTypeParams = Map("version" -> "0.0.4")
  private[this] val mediaType = MediaType.customWithFixedCharset(
    "text", "plain", HttpCharsets.`UTF-8`, params = mediaTypeParams)

  private[stats] def fromRegistry(
    collectorRegistry: CollectorRegistry): MetricsEntity = {
    MetricsEntity(collectorRegistry.metricFamilySamples())
  }

  private[stats] def toPrometheusTextFormat(e: MetricsEntity): String = {
    val writer: Writer = new StringWriter()
    TextFormat.write004(writer, e.samples)

    writer.toString
  }

  implicit val metricsMarshaller: ToEntityMarshaller[MetricsEntity] = {
    Marshaller.withFixedContentType(mediaType) { s =>
      HttpEntity(mediaType, toPrometheusTextFormat(s))
    }
  }
}
