package me.amanj.greenish.stats

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.concurrent.Eventually
import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.duration._
import scala.language.postfixOps
import me.amanj.greenish.models._
import io.prometheus.client.Collector.MetricFamilySamples
import java.io.File
import scala.jdk.CollectionConverters._

class StatsCollectorSpec()
    extends TestKit(ActorSystem("StatsCollectorSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Eventually {

  import StatsCollectorSpec._

  "StatsCollector" must {
    "initialize labels upon instantiation" in {
      val jobs = Set("p1", "p2")
      val stats = system.actorOf(
        Props(new StatsCollector(jobs)))

      stats ! GetPrometheus

      val received = receiveOne(2 seconds)
      assert(received.isInstanceOf[StatsCollector.MetricsEntity])

      val prometheus = received
        .asInstanceOf[StatsCollector.MetricsEntity]
        .samples
        .asScala
        .toList

      prometheus.isEmpty shouldBe false
      prometheus.foreach { prom =>
        val labels = prom.samples.asScala
          .flatMap(_.labelValues.asScala)
          .filter(jobs.contains(_))
          .toSet
        labels shouldBe jobs
      }
    }

    "properly handle IncRefresh message" in {
      val jobs = Set("p1", "p2")
      val stats = system.actorOf(
        Props(new StatsCollector(jobs)))

      stats ! IncRefresh("p2")
      stats ! GetPrometheus

      val expected = Seq(
        (Seq("p1"), 0.0),
        (Seq("p2"), 1.0),
      )

      val prom = receiveOne(2 seconds)
        .asInstanceOf[StatsCollector.MetricsEntity]
        .samples.asScala.toList

      checkSamples(prom, "greenish_state_refresh_total", expected)
      checkSamples(prom, "greenish_active_refresh_tasks", expected)
    }

    "properly handle DecRefresh message" in {
      val jobs = Set("p1", "p2")
      val stats = system.actorOf(
        Props(new StatsCollector(jobs)))

      stats ! IncRefresh("p1")
      stats ! DecRefresh("p1")
      stats ! GetPrometheus

      val expectedTotal = Seq(
        (Seq("p1"), 1.0),
        (Seq("p2"), 0.0),
      )

      val expectedActive = Seq(
        (Seq("p1"), 0.0),
        (Seq("p2"), 0.0),
      )

      val prom = receiveOne(2 seconds)
        .asInstanceOf[StatsCollector.MetricsEntity]
        .samples.asScala.toList

      checkSamples(prom, "greenish_state_refresh_total", expectedTotal)
      checkSamples(prom, "greenish_active_refresh_tasks", expectedActive)
    }

    "properly handle IncBadRefresh message" in {
      val jobs = Set("p1", "p2")
      val stats = system.actorOf(
        Props(new StatsCollector(jobs)))

      stats ! IncBadRefresh("p1")
      stats ! GetPrometheus

      val expected = Seq(
        (Seq("p1"), 1.0),
        (Seq("p2"), 0.0),
      )

      val prom = receiveOne(2 seconds)
        .asInstanceOf[StatsCollector.MetricsEntity]
        .samples.asScala.toList

      checkSamples(prom, "greenish_state_refresh_failed_total", expected)
    }

    "properly handle OldestMissingPeriod message" in {
      val jobs = Set("p1", "p2")
      val stats = system.actorOf(
        Props(new StatsCollector(jobs)))

      stats ! OldestMissingPeriod("p1", 3)
      stats ! GetPrometheus

      val expected = Seq(
        (Seq("p1"), 3.0),
        (Seq("p2"), 0.0),
      )

      val prom = receiveOne(2 seconds)
        .asInstanceOf[StatsCollector.MetricsEntity]
        .samples.asScala.toList

      checkSamples(prom, "greenish_oldest_missing_period", expected)
    }

    "properly handle MissingPeriods message" in {
      val jobs = Set("p1", "p2")
      val stats = system.actorOf(
        Props(new StatsCollector(jobs)))

      stats ! MissingPeriods("p1", 3)
      stats ! GetPrometheus

      val expected = Seq(
        (Seq("p1"), 3.0),
        (Seq("p2"), 0.0),
      )

      val prom = receiveOne(2 seconds)
        .asInstanceOf[StatsCollector.MetricsEntity]
        .samples.asScala.toList

      checkSamples(prom, "greenish_missing_periods_total", expected)
    }

    "properly handle RefreshTime message" in {
      val jobs = Set("p1", "p2")
      val stats = system.actorOf(
        Props(new StatsCollector(jobs)))

      stats ! RefreshTime("p1", 3)
      stats ! GetPrometheus

      val expected = Set("p1")

      val prom = receiveOne(2 seconds)
        .asInstanceOf[StatsCollector.MetricsEntity]
        .samples.asScala.toList

      val actual =
        getNoneZeroHistogramLabels(prom,
          "greenish_state_refresh_time_seconds")
      actual shouldBe expected
    }
  }
}

object StatsCollectorSpec extends Matchers {
  def getNoneZeroHistogramLabels(
    prom: List[MetricFamilySamples],
    name: String): Set[String] =
      prom.filter { prom =>
          prom.name == name
        }.flatMap { metric =>
          metric.samples.asScala
            .map(sample => (sample.labelValues.asScala, sample.value))
        }.filter { case (seq, num) =>
          // Only keep what is set
          num != 0
        }.map { case (seq, num) => seq.head }
        .toSet


  def checkSamples(
    prom: List[MetricFamilySamples],
    name: String,
    expected: Seq[(Seq[String], Double)]): Unit = {

    val actual = prom
      .filter { prom =>
        prom.name == name
      }.flatMap { metric =>
        metric.samples.asScala
          .map(sample => (sample.labelValues.asScala, sample.value))
      }

    actual shouldBe expected
  }
}
