package me.amanj.greenish.checker

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
import java.io.File

class StatusCheckerSpec()
    extends TestKit(ActorSystem("StatusCheckerSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Eventually {

  override def afterAll: Unit = {
    dir1.delete
    dir2.delete
    dir3.delete
    dir4.delete
    TestKit.shutdownActorSystem(system)
  }

  override def beforeAll: Unit = {
    dir1.mkdirs
    dir2.mkdirs
    dir3.mkdirs
    dir4.mkdirs
  }

  val dir1 = new File("/tmp/job1/2020-06-25-14")
  val dir2 = new File("/tmp/job3/2020-06-25-14")
  val dir3 = new File("/tmp/job4/2020-06-25-13")
  val dir4 = new File("/tmp/job4/2020-06-25-15")


  implicit val patience: PatienceConfig = PatienceConfig(1 minute, 1 second)

  val lsScript = getClass.getResource("/test-ls").getFile
  val lsEnvScript = getClass.getResource("/test-ls-env").getFile

  val job1 = Job(1, "job1", s"$lsScript /tmp/job1",
      "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
      2, AlertLevels(0, 1, 2, 3),
    )

  val job2 = Job(2, "job2", s"$lsScript /tmp/job2",
      "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
      1, AlertLevels(1, 2, 3, 4),
    )

  val job3 = Job(3, "job3", s"$lsScript /tmp/job3",
      "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
      3, AlertLevels(0, 1, 2, 3),
    )

  val job4 = Job(4, "job4", s"$lsEnvScript job4",
      "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
      4, AlertLevels(0, 1, 2, 3),
    )

  val group1 = Group(1, "group1", Seq(job1, job2))
  val group2 = Group(2, "group2", Seq(job3, job4))

  val groups = Seq(group1, group2)

  val emptyChecker = new StatusCheckerApi {
    override protected[this] var state = Seq.empty[GroupStatus]
  }

  val singletonChecker = new StatusCheckerApi {
    override protected[this] var state = Seq(GroupStatus(
        group1, Seq(JobStatus(job1, Seq(PeriodHealth("1", false))))
      ))
  }

  val nestedChecker = new StatusCheckerApi {
    override protected[this] var state = Seq(
      GroupStatus(
        group1, Seq(
          JobStatus(job1, Seq(PeriodHealth("1", true), PeriodHealth("1", true))),
          JobStatus(job1, Seq(PeriodHealth("2", false), PeriodHealth("3", false))),
        )
      ),
      GroupStatus(
        group1, Seq(
          JobStatus(job1, Seq(PeriodHealth("1", false), PeriodHealth("1", true))),
          JobStatus(job1, Seq(PeriodHealth("2", false),
            PeriodHealth("3", false), PeriodHealth("4", false))),
        )
      )
    )
  }

  "maxLag" must {

    "work when state is empty" in {
      emptyChecker.maxLag shouldBe 0
    }

    "work when state is not empty" in {
      singletonChecker.maxLag shouldBe 1
    }

    "work when state is deeply nested" in {
      nestedChecker.maxLag shouldBe 3
    }
  }

  "allEntries" must {

    "work when state is empty" in {
      emptyChecker.allEntries.length shouldBe 0
    }

    "work when state is not empty" in {
      singletonChecker.allEntries.length shouldBe 1
    }

    "work when state is deeply nested" in {
      nestedChecker.allEntries.length shouldBe 2
    }
  }

  "getMissing" must {

    "work when state is empty" in {
      emptyChecker.getMissing shouldBe Seq.empty
    }

    "work when state is not empty" in {
      val expected = Seq(GroupStatus(
        group1, Seq(JobStatus(job1, Seq(PeriodHealth("1", false))))
      ))

      singletonChecker.getMissing shouldBe expected
    }

    "work when state is deeply nested" in {
      val expected = Seq(
        GroupStatus(
          group1, Seq(
            JobStatus(job1, Seq(PeriodHealth("2", false), PeriodHealth("3", false))),
          )
        ),
        GroupStatus(
          group1, Seq(
            JobStatus(job1, Seq(PeriodHealth("1", false))),
            JobStatus(job1, Seq(PeriodHealth("2", false),
              PeriodHealth("3", false), PeriodHealth("4", false))),
          )
        )
      )

      nestedChecker.getMissing shouldBe expected
    }
  }

  "summary" must {

    "work when state is empty" in {
      emptyChecker.summary shouldBe Seq.empty
    }

    "work when state is not empty" in {
      val expected = Seq(
          GroupStatusSummary("group1", Seq(JobStatusSummary("job1", 1, Normal)))
        )
      singletonChecker.summary shouldBe expected
    }

    "work when state is deeply nested" in {
      val expected = Seq(
        GroupStatusSummary(
          "group1", Seq(
            JobStatusSummary("job1", 0, Great),
            JobStatusSummary("job1", 2, Warn),
          )
        ),
        GroupStatusSummary(
          "group1", Seq(
            JobStatusSummary("job1", 1, Normal),
            JobStatusSummary("job1", 3, Critical),
          )
        )
      )
      nestedChecker.summary shouldBe expected
    }
  }

  "periods" must {

    "work for hourly frequency" in {
      val job = Job(1, null, null,
        "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
        3, null,
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2020-06-25-13", "2020-06-25-14", "2020-06-25-15")
      actual shouldBe expected
    }

    "respect job timezone" in {
      val job = Job(1, null, null,
        "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
        3, null,
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[Africa/Cairo]"))

      val expected = Seq("2020-06-25-11", "2020-06-25-12", "2020-06-25-13")
      actual shouldBe expected
    }

    "work for daily frequency" in {
      val job = Job(1, null, null,
        "yyyy-MM-dd", Daily, ZoneId.of("UTC"),
        3, null,
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2020-06-23", "2020-06-24", "2020-06-25")
      actual shouldBe expected
    }

    "work for monthly frequency" in {
      val job = Job(1, null, null,
        "yyyy-MM-01", Monthly, ZoneId.of("UTC"),
        3, null,
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2020-04-01", "2020-05-01", "2020-06-01")
      actual shouldBe expected
    }

    "work for yearly frequency" in {
      val job = Job(1, null, null,
        "yyyy-01-01", Yearly, ZoneId.of("UTC"),
        3, null,
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2018-01-01", "2019-01-01", "2020-01-01")
      actual shouldBe expected
    }
  }

  "Refresh" must {
    "work" in {
      // val actual =
      //   StatusChecker.periods(job, )
      val now = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      val actor = system.actorOf(Props(
        new StatusChecker(groups, Seq("GREENISH_VALUE_FOR_TEST" -> "/tmp"))))

      val expected = List(
        GroupStatus(
          group1,
          List(
            JobStatus(
              job1,
              Vector(
                PeriodHealth("2020-06-25-14", true),
                PeriodHealth("2020-06-25-15", false))),
            JobStatus(
              job2,
              Vector(
                PeriodHealth("2020-06-25-15", false))))),
        GroupStatus(
          group2,
          List(
            JobStatus(
              job3,
              Vector(
                PeriodHealth("2020-06-25-13", false),
                PeriodHealth("2020-06-25-14", true),
                PeriodHealth("2020-06-25-15", false))),
            JobStatus(
              job4,
              Vector(
                PeriodHealth("2020-06-25-12", false),
                PeriodHealth("2020-06-25-13", true),
                PeriodHealth("2020-06-25-14", false),
                PeriodHealth("2020-06-25-15", true))))))

      actor ! Refresh(() => now)

      eventually {
        actor ! AllEntries
        val msg = receiveOne(5 second)
        msg shouldBe expected
      }
    }
  }
}
