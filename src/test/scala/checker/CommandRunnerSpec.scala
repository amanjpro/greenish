package me.amanj.greenish.checker

import akka.actor.{ActorSystem, Props}
import java.io.File
import scala.concurrent.duration._
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import me.amanj.greenish.models.PeriodHealth

import scala.language.postfixOps

class CommandRunnerSpec()
    extends TestKit(ActorSystem("CommandRunnerSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  val dir = new File("/tmp/2020-06-07-01")
  val dirWithSpaces = new File("/tmp/2020-06-07 01")
  val ls = getClass.getResource("/test-ls").getFile
  val lsEnv = getClass.getResource("/test-ls-env").getFile
  val lsDup = getClass.getResource("/test-duplicate-period").getFile
  val lsPart = getClass.getResource("/test-partial-period").getFile
  override def beforeAll: Unit = {
    dirWithSpaces.mkdirs
    dir.mkdirs
  }

  override def afterAll: Unit = {
    dir.delete
    dirWithSpaces.delete
    TestKit.shutdownActorSystem(system)
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

    "send back nothing, when command does not exit" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! BatchRun("a;kjdw", Seq.empty, Seq.empty, 0, 0, 0)
      expectNoMessage(4 seconds)
    }

    "send back nothing, when command does not exit with 0" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! BatchRun("exit 1;", Seq.empty, Seq.empty, 0, 0, 0)
      expectNoMessage(4 seconds)
    }

    "send back nothing, when command exits with 0, but not all periods are printed" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! BatchRun(lsPart, Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, 2)
      expectNoMessage(4 seconds)
    }

    "send back nothing, when command exits with 0, but some periods are printed more than once" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! BatchRun(lsDup, Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, 2)
      expectNoMessage(4 seconds)
    }

    "send back health for all periods, when command does exit with 0 with all periods printed exactly once" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! BatchRun(s"$ls /tmp", Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, 2)
      val expected = RunResult(Seq(
        PeriodHealth("2020-06-07-01", true),
        PeriodHealth("2020-06-07-02", false)), 0, 1, 2)
      expectMsg(expected)
    }

    "Support spaces in the period pattern" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! BatchRun(s"$ls /tmp", Seq("2020-06-07 01", "2020-06-07 02"), Seq.empty, 0, 1, 2)
      val expected = RunResult(Seq(
        PeriodHealth("2020-06-07 01", true),
        PeriodHealth("2020-06-07 02", false)), 0, 1, 2)
      expectMsg(expected)
    }

    "use provided environment variables" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! BatchRun(s"$lsEnv .", Seq("2020-06-07-01", "2020-06-07-02"), Seq.empty, 0, 1, 2)
      val expected1 = RunResult(Seq(
        PeriodHealth("2020-06-07-01", false),
        PeriodHealth("2020-06-07-02", false)), 0, 1, 2)
      expectMsg(expected1)

      actor ! BatchRun(s"$lsEnv .", Seq("2020-06-07-01", "2020-06-07-02"),
        Seq("GREENISH_VALUE_FOR_TEST" -> "/tmp"), 0, 1, 2)
      val expected2 = RunResult(Seq(
        PeriodHealth("2020-06-07-01", true),
        PeriodHealth("2020-06-07-02", false)), 0, 1, 2)
      expectMsg(expected2)
    }
  }
}
