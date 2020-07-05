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
  val ls = getClass.getResource("/test-ls").getFile
  val lsEnv = getClass.getResource("/test-ls-env").getFile
  val lsDup = getClass.getResource("/test-duplicate-period").getFile
  val lsPart = getClass.getResource("/test-partial-period").getFile
  override def beforeAll: Unit = {
    dir.mkdirs
  }

  override def afterAll: Unit = {
    dir.delete
    TestKit.shutdownActorSystem(system)
  }

  "Run command" must {

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
