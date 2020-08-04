package me.amanj.greenish.checker

import akka.actor.{ActorSystem, Props, ActorRef}
import java.io.File
import scala.concurrent.duration._
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.concurrent.Eventually
import scala.concurrent.duration._
import me.amanj.greenish.models.PeriodHealth
import me.amanj.greenish.stats.{StatsCollector, StatsCollectorSpec, GetPrometheus}
import scala.jdk.CollectionConverters._

import scala.language.postfixOps

class CommandRunnerSpec()
    extends TestKit(ActorSystem("CommandRunnerSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with Eventually
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  val farFuture = System.currentTimeMillis * 2
  val dir = new File("/tmp/2020-06-07-01")
  val dirWithSpaces = new File("/tmp/2020-06-07 01")
  val lsSleep = getClass.getResource("/ls-sleep").getFile
  val ls = getClass.getResource("/test-ls").getFile
  val lsEnv = getClass.getResource("/test-ls-env").getFile
  val lsDup = getClass.getResource("/test-duplicate-period").getFile
  val lsPart = getClass.getResource("/test-partial-period").getFile
  implicit val patience: PatienceConfig = PatienceConfig(15 seconds, 1 second)

  var stats: ActorRef = _

  override def beforeAll: Unit = {
    dirWithSpaces.mkdirs
    dir.mkdirs
  }

  override def afterAll: Unit = {
    dir.delete
    dirWithSpaces.delete
    TestKit.shutdownActorSystem(system)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    stats = system.actorOf(
      Props(new StatsCollector(Set("p1", "p2", "p3"))))
  }

  "parseOutput" must {
    "parse output lines correctly" in {
      val lines = LazyList(
        "greenish-period\t2020-02-17 8\t1",
        "greenish-period\t2020-02-17-9\t1",
        "greenish-period\t2020-02-17 10\t0",
        "greenish-period\t2020-02-17-11\t0",
        "greenish-period\t2020-02-17 10 38\t0",
        "Other output",
        "greenish-period 2020-02-17 10 38 0",
        "greenish-period\t2020-02-17 10 38\t9",
      )
      val periods = Set(
        "2020-02-17 8",
        "2020-02-17-9",
        "2020-02-17 10",
        "2020-02-17-11",
        )

      val expected = Seq(
        ("2020-02-17 8", true),
        ("2020-02-17-9", true),
        ("2020-02-17 10", false),
        ("2020-02-17-11", false),
        )

      val actual = CommandRunner.parseOutput(lines, periods)

      actual shouldBe expected
    }

    "ignore lines that do not match the period set" in {
      val lines = LazyList(
        "greenish-period\t2020-02-17-10\t1",
        "greenish-period\t2020-02-17-11\t0",
      )
      val periods = Set(
        "2020-02-17-10",
        )

      val expected = Seq(
        ("2020-02-17-10", true),
        )

      val actual = CommandRunner.parseOutput(lines, periods)

      actual shouldBe expected
    }

    "capture duplicate periods correctly" in {
      val lines = LazyList(
        "greenish-period\t2020-02-17-10\t1",
        "greenish-period\t2020-02-17-10\t0",
        "greenish-period\t2020-02-17-11\t0",
      )
      val periods = Set(
        "2020-02-17-10",
        "2020-02-17-11",
        )

      val expected = Seq(
        ("2020-02-17-10", true),
        ("2020-02-17-10", false),
        ("2020-02-17-11", false),
        )

      val actual = CommandRunner.parseOutput(lines, periods)

      actual shouldBe expected
    }

    "Have no problem if a period in the provided period-set wasn't in the output lines" in {
      val lines = LazyList(
        "greenish-period\t2020-02-17-10\t1",
        "greenish-period\t2020-02-17-11\t0",
      )
      val periods = Set(
        "2020-02-17-10",
        "2020-02-17-11",
        "2020-02-17-12",
        )

      val expected = Seq(
        ("2020-02-17-10", true),
        ("2020-02-17-11", false),
        )

      val actual = CommandRunner.parseOutput(lines, periods)

      actual shouldBe expected
    }
  }

  "toBashCommand" must {
    "single-quote the periods to avoid bash splitting" in {
      val periods = Seq("20 02", "30 03", "01 10", "400")
      val cmd = "hey this is a command"
      val actual = CommandRunner.toBashCommand(cmd, periods)
      val expected = "hey this is a command '20 02' '30 03' '01 10' '400'"
      actual shouldBe expected
    }
  }

  "BatchRun command" must {

    import StatsCollectorSpec.{checkSamples, getNoneZeroHistogramLabels}

    "not run anything if the refresh command is too old" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(lsPart, Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p1", 2, System.currentTimeMillis)
      expectNoMessage(4 seconds)
    }

    "send back nothing, when command does not exit" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun("a;kjdw", Seq.empty, Seq.empty, 0, 0, "p1", 0, farFuture)
      expectNoMessage(4 seconds)
    }

    "send back nothing, when command does not exit with 0" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun("exit 1;", Seq.empty, Seq.empty, 0, 0, "p1", 0, farFuture)
      expectNoMessage(4 seconds)
    }

    "send back nothing, when command exits with 0, but not all periods are printed" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(lsPart, Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p1", 2, farFuture)
      expectNoMessage(4 seconds)
    }

    "send back nothing, when command exits with 0, but some periods are printed more than once" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(lsDup, Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p1", 2, farFuture)
      expectNoMessage(4 seconds)
    }

    "send back health for all periods, when command does exit with 0 with all periods printed exactly once" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(s"$ls /tmp", Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p1", 2, farFuture)
      val expected = RunResult(Seq(
        PeriodHealth("2020-06-07-01", true),
        PeriodHealth("2020-06-07-02", false)), 0, 1, 2)
      expectMsg(expected)
    }

    "Support spaces in the period pattern" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(s"$ls /tmp", Seq("2020-06-07 01", "2020-06-07 02"), Seq.empty, 0, 1, "p1", 2, farFuture)
      val expected = RunResult(Seq(
        PeriodHealth("2020-06-07 01", true),
        PeriodHealth("2020-06-07 02", false)), 0, 1, 2)
      expectMsg(expected)
    }

    "use provided environment variables" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(s"$lsEnv .", Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p1", 2, farFuture)
      val expected1 = RunResult(Seq(
        PeriodHealth("2020-06-07-01", false),
        PeriodHealth("2020-06-07-02", false)), 0, 1, 2)
      expectMsg(expected1)

      actor ! BatchRun(s"$lsEnv .", Seq("2020-06-07-01", "2020-06-07-02"),
        Seq("GREENISH_VALUE_FOR_TEST" -> "/tmp"), 0, 1, "p1", 2,
        farFuture)
      val expected2 = RunResult(Seq(
        PeriodHealth("2020-06-07-01", true),
        PeriodHealth("2020-06-07-02", false)), 0, 1, 2)
      expectMsg(expected2)
    }

    "correctly send stats when command run fails" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(
        s"exit 1", Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p1", 2, farFuture)

      eventually {
        stats ! GetPrometheus

        val expectedTotal = Seq(
          (Seq("p1"), 1.0),
          (Seq("p2"), 0.0),
          (Seq("p3"), 0.0),
        )

        val allZeros = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 0.0),
          (Seq("p3"), 0.0),
        )

        val prom = receiveOne(2 seconds)
          .asInstanceOf[StatsCollector.MetricsEntity]
          .samples.asScala.toList

        checkSamples(prom, "greenish_state_refresh_total", expectedTotal)
        checkSamples(prom, "greenish_state_refresh_failed_total", expectedTotal)
        checkSamples(prom, "greenish_missing_periods_total", allZeros)
        checkSamples(prom, "greenish_oldest_missing_period", allZeros)
        checkSamples(prom, "greenish_active_refresh_tasks", allZeros)

        val actual = getNoneZeroHistogramLabels(prom, "greenish_state_refresh_time_seconds")
        actual shouldBe Set("p1")
      }
    }

    "correctly send stats when command run succeeds" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(
        s"$ls /tmp", Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p2", 2, farFuture)

      eventually {
        stats ! GetPrometheus

        val expectedTotal = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 1.0),
          (Seq("p3"), 0.0),
        )

        val allZeros = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 0.0),
          (Seq("p3"), 0.0),
        )

        val prom = receiveOne(2 seconds)
          .asInstanceOf[StatsCollector.MetricsEntity]
          .samples.asScala.toList

        checkSamples(prom, "greenish_state_refresh_total", expectedTotal)
        checkSamples(prom, "greenish_state_refresh_failed_total", allZeros)
        checkSamples(prom, "greenish_missing_periods_total", expectedTotal)
        checkSamples(prom, "greenish_oldest_missing_period", expectedTotal)
        checkSamples(prom, "greenish_active_refresh_tasks", allZeros)

        val actual = getNoneZeroHistogramLabels(prom, "greenish_state_refresh_time_seconds")
        actual shouldBe Set("p2")
      }
    }

    "correctly send stats when command run misses some periods" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(
        lsPart, Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p2", 2, farFuture)

      eventually {
        stats ! GetPrometheus

        val expectedTotal = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 1.0),
          (Seq("p3"), 0.0),
        )

        val allZeros = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 0.0),
          (Seq("p3"), 0.0),
        )

        val prom = receiveOne(2 seconds)
          .asInstanceOf[StatsCollector.MetricsEntity]
          .samples.asScala.toList

        checkSamples(prom, "greenish_state_refresh_total", expectedTotal)
        checkSamples(prom, "greenish_state_refresh_failed_total", expectedTotal)
        checkSamples(prom, "greenish_missing_periods_total", allZeros)
        checkSamples(prom, "greenish_oldest_missing_period", allZeros)
        checkSamples(prom, "greenish_active_refresh_tasks", allZeros)

        val actual = getNoneZeroHistogramLabels(prom, "greenish_state_refresh_time_seconds")
        actual shouldBe Set("p2")
      }
    }

    "correctly send stats when command run prints duplicate periods" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(
        lsDup, Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p2", 2, farFuture)

      eventually {
        stats ! GetPrometheus

        val expectedTotal = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 1.0),
          (Seq("p3"), 0.0),
        )

        val allZeros = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 0.0),
          (Seq("p3"), 0.0),
        )

        val prom = receiveOne(2 seconds)
          .asInstanceOf[StatsCollector.MetricsEntity]
          .samples.asScala.toList

        checkSamples(prom, "greenish_state_refresh_total", expectedTotal)
        checkSamples(prom, "greenish_state_refresh_failed_total", expectedTotal)
        checkSamples(prom, "greenish_missing_periods_total", allZeros)
        checkSamples(prom, "greenish_oldest_missing_period", allZeros)
        checkSamples(prom, "greenish_active_refresh_tasks", allZeros)

        val actual = getNoneZeroHistogramLabels(prom, "greenish_state_refresh_time_seconds")
        actual shouldBe Set("p2")
      }
    }

    "correctly compute active refresh stats" in {
      val actor = system.actorOf(Props(new CommandRunner(stats)))
      actor ! BatchRun(
        lsSleep, Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, "p3", 2, farFuture)

      eventually {
        stats ! GetPrometheus

        val expected = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 0.0),
          (Seq("p3"), 1.0),
        )

        val prom = receiveOne(2 seconds)
          .asInstanceOf[StatsCollector.MetricsEntity]
          .samples.asScala.toList

        checkSamples(prom, "greenish_active_refresh_tasks", expected)
      }

      eventually {
        stats ! GetPrometheus

        val expected = Seq(
          (Seq("p1"), 0.0),
          (Seq("p2"), 0.0),
          (Seq("p3"), 0.0),
        )

        val prom = receiveOne(2 seconds)
          .asInstanceOf[StatsCollector.MetricsEntity]
          .samples.asScala.toList

        checkSamples(prom, "greenish_active_refresh_tasks", expected)
      }
    }
  }
}
