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
import me.amanj.greenish.stats._
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

  val farFuture = System.currentTimeMillis * 2
  val tstamp = 2000L
  val dir1 = new File("/tmp/job1/2020-06-25-14")
  val dir2 = new File("/tmp/job3/2020-06-25-14")
  val dir3 = new File("/tmp/job4/2020-06-25-13")
  val dir4 = new File("/tmp/job4/2020-06-25-14")

  val stats = system.actorOf(
    Props(new StatsCollector(Set("p1", "p2", "p3"))))

  implicit val patience: PatienceConfig = PatienceConfig(15 seconds, 1 second)

  val lsScript = getClass.getResource("/test-ls").getFile
  val lsEnvScript = getClass.getResource("/test-ls-env").getFile

  val job1 = Job(0, "job1", "p1", s"$lsScript /tmp/job1",
      "yyyy-MM-dd-HH", Hourly, 1, ZoneId.of("UTC"),
      2, AlertLevels(0, 1, 2, 3),
      Seq("GREENISH_VALUE_FOR_TEST" -> "/tmp"),
    )

  val job2 = Job(1, "job2", "p2", s"$lsScript /tmp/job2",
      "yyyy-MM-dd-HH", Hourly, 2, ZoneId.of("UTC"),
      1, AlertLevels(1, 2, 3, 4),
      Seq("GREENISH_VALUE_FOR_TEST" -> "/tmp"),
    )

  val job3 = Job(0, "job3", "p3", s"$lsScript /tmp/job3",
      "yyyy-MM-dd-HH", Hourly, 1, ZoneId.of("UTC"),
      3, AlertLevels(0, 1, 2, 3),
      Seq("GREENISH_VALUE_FOR_TEST" -> "/tmp"),
    )

  val job4 = Job(1, "job4", "p4", s"$lsEnvScript job4",
      "yyyy-MM-dd-HH", Hourly, 1, ZoneId.of("UTC"),
      4, AlertLevels(0, 1, 2, 3),
      Seq("GREENISH_VALUE_FOR_TEST" -> "/tmp"),
    )

  val group1 = Group(0, "group1", Seq(job1, job2))
  val group2 = Group(1, "group2", Seq(job3, job4))

  val groups = Seq(group1, group2)

  val emptyChecker = new StatusCheckerApi {
    override protected[this] var state = IndexedSeq.empty[GroupStatus]
  }

  val singletonState = IndexedSeq(GroupStatus(
    group1, Array(JobStatus(job1, tstamp, Seq(PeriodHealth("1", false))))
  ))

  val singletonChecker = new StatusCheckerApi {
    override protected[this] var state = singletonState
  }

  val deeplyNestedState = IndexedSeq(
    GroupStatus(
      group1, Array(
        JobStatus(job1, tstamp, Seq(PeriodHealth("1", true), PeriodHealth("1", true))),
        JobStatus(job1, tstamp, Seq(PeriodHealth("2", false), PeriodHealth("3", false))),
      )
    ),
    GroupStatus(
      group1, Array(
        JobStatus(job1, tstamp, Seq(PeriodHealth("1", false), PeriodHealth("1", true))),
        JobStatus(job1, tstamp, Seq(PeriodHealth("2", false),
          PeriodHealth("3", false), PeriodHealth("4", false))),
      )
    )
  )

  val nestedChecker = new StatusCheckerApi {
    override protected[this] var state = deeplyNestedState
  }

  "maxLag" must {

    "work when state is empty" in {
      emptyChecker.maxLag shouldBe Lag(0)
    }

    "work when state is not empty" in {
      singletonChecker.maxLag shouldBe Lag(1)
    }

    "work when state is deeply nested" in {
      nestedChecker.maxLag shouldBe Lag(3)
    }
  }

  "allEntries" must {

    "work when state is empty" in {
      emptyChecker.allEntries.length shouldBe 0
    }

    "work when state is not empty" in {
      singletonChecker.allEntries shouldBe singletonState
    }

    "work when state is deeply nested" in {
      nestedChecker.allEntries shouldBe deeplyNestedState
    }
  }

  "getMissing" must {

    "work when state is empty" in {
      emptyChecker.getMissing shouldBe Seq.empty
    }

    "work when state is not empty" in {
      val expected = Seq(GroupStatus(
        group1, Array(JobStatus(job1, tstamp, Seq(PeriodHealth("1", false))))
      ))

      singletonChecker.getMissing shouldBe expected
    }

    "work when state is deeply nested" in {
      val expected = Seq(
        GroupStatus(
          group1, Array(
            JobStatus(job1, tstamp, Seq(PeriodHealth("2", false), PeriodHealth("3", false))),
          )
        ),
        GroupStatus(
          group1, Array(
            JobStatus(job1, tstamp, Seq(PeriodHealth("1", false))),
            JobStatus(job1, tstamp, Seq(PeriodHealth("2", false),
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
          GroupStatusSummary(0, "group1", Seq(JobStatusSummary(0, "job1", 1, 1, Normal)))
        )
      singletonChecker.summary shouldBe expected
    }

    "work when state is deeply nested" in {
      val expected = Seq(
        GroupStatusSummary(
          0, "group1", Seq(
            JobStatusSummary(0, "job1", 0, 0, Great),
            JobStatusSummary(0, "job1", 2, 2, Warn),
          )
        ),
        GroupStatusSummary(
          0, "group1", Seq(
            JobStatusSummary(0, "job1", 1, 2, Normal),
            JobStatusSummary(0, "job1", 3, 3, Critical),
          )
        )
      )
      nestedChecker.summary shouldBe expected
    }
  }

  "getGroupStatus" must {

    "return None when state is empty" in {
      emptyChecker.getGroupStatus(0) shouldBe None
    }

    "return Some when state is not empty and groupId exists" in {
      val expected = Some(GroupStatus(group1, Array(
        JobStatus(job1, tstamp, Seq(PeriodHealth("1", false))))))
      singletonChecker.getGroupStatus(0) shouldBe expected
    }

    "return None when state is not empty and groupId does not exist" in {
      singletonChecker.getGroupStatus(2) shouldBe None
    }
  }

  "getJobStatus" must {

    "return None when state is empty" in {
      emptyChecker.getJobStatus(0, 0) shouldBe None
    }

    "return None when state is not empty and groupId does not exist" in {
      singletonChecker.getJobStatus(2, 0) shouldBe None
    }

    "return None when state is not empty and groupId exists but jobId does not exist" in {
      singletonChecker.getJobStatus(0, 2) shouldBe None
    }

    "return Some when state is not empty and groupId and jobId exist" in {
      val expected = Some(JobStatus(job1, tstamp, Seq(PeriodHealth("1", false))))
      singletonChecker.getJobStatus(0, 0) shouldBe expected
    }
  }

  "initState" must {

    "work when groups is empty" in {
      StatusChecker.initState(Seq.empty)shouldBe IndexedSeq.empty
    }

    "work when groups is not empty" in {
      val expected = IndexedSeq(
          GroupStatus(group1, Array(
            JobStatus(job1, -1, Seq.empty),
            JobStatus(job2, -1, Seq.empty),
            ))
        )
      StatusChecker.initState(Seq(group1)) shouldBe expected
    }

    "work when groups is deeply nested" in {
      val expected = IndexedSeq(
          GroupStatus(group1, Array(
            JobStatus(job1, -1, Seq.empty),
            JobStatus(job2, -1, Seq.empty),
            )),
          GroupStatus(group2, Array(
            JobStatus(job3, -1, Seq.empty),
            JobStatus(job4, -1, Seq.empty),
            )),
        )
      StatusChecker.initState(groups) shouldBe expected
    }
  }

  "periods" must {
    "work when job's period-check-offset is 0" in {
      val job = Job(1, null, null, null,
        "yyyy-MM-dd-HH", Hourly, 0, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2020-06-25-13", "2020-06-25-14", "2020-06-25-15")
      actual shouldBe expected
    }

    "work when job's period-check-offset is not 0" in {
      val job = Job(1, null, null, null,
        "yyyy-MM-dd-HH", Hourly, 3, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2020-06-25-10", "2020-06-25-11", "2020-06-25-12")
      actual shouldBe expected
    }

    "work various cron styles" in {
      val job = cron => Job(1, null, null, null,
        "yyyy-MM-dd-HH-mm", cron, 1, ZoneId.of("UTC"),
        2, null, Seq.empty
      )

      val cronPeriods = Seq (
         "* * * * *" -> Seq("2020-06-25-15-04", "2020-06-25-15-05"),
         "0 * * * *" -> Seq("2020-06-25-14-00", "2020-06-25-15-00"),
         "0 1 * * MON-TUE" -> Seq("2020-06-22-01-00", "2020-06-23-01-00"),
         "0 1 1 1 *" -> Seq("2019-01-01-01-00", "2020-01-01-01-00"),
         "0 1 1 JAN-FEB *" -> Seq("2020-01-01-01-00", "2020-02-01-01-00"),
        )

      val time = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      cronPeriods.foreach { case (cron, expected) =>
        val actual = StatusChecker.periods(job(Cron(cron)), time)
        actual shouldBe expected
      }
    }

    "work for hourly frequency" in {
      val job = Job(1, null, null, null,
        "yyyy-MM-dd-HH", Hourly, 1, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2020-06-25-12", "2020-06-25-13", "2020-06-25-14")
      actual shouldBe expected
    }

    "respect job timezone" in {
      val job = Job(1, null, null, null,
        "yyyy-MM-dd-HH", Hourly, 1, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[Africa/Cairo]"))

      val expected = Seq("2020-06-25-10", "2020-06-25-11", "2020-06-25-12")
      actual shouldBe expected
    }

    "work for daily frequency" in {
      val job = Job(1, null, null, null,
        "yyyy-MM-dd", Daily, 1, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2020-06-22", "2020-06-23", "2020-06-24")
      actual shouldBe expected
    }

    "work for monthly frequency" in {
      val job = Job(1, null, null, null,
        "yyyy-MM-01", Monthly, 0, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2020-04-01", "2020-05-01", "2020-06-01")
      actual shouldBe expected
    }

    "work for yearly frequency" in {
      val job = Job(1, null, null, null,
        "yyyy-01-01", Annually, 1, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.periods(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = Seq("2017-01-01", "2018-01-01", "2019-01-01")
      actual shouldBe expected
    }
  }

  "nowMinusOffset" must {
    "work with UTC when offset is zero" in {
      val job = Job(0, null, null, null,
        "yyyy-01-01", Hourly, 0, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.nowMinusOffset(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      actual shouldBe expected
    }

    "work with UTC when offset is not zero" in {
      val job = Job(1, null, null, null,
        "yyyy-01-01", Annually, 1, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.nowMinusOffset(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))

      val expected = ZonedDateTime.parse("2019-06-25T15:05:30+01:00[UTC]")
      actual shouldBe expected
    }

    "work with non-UTC when offset is zero" in {
      val job = Job(0, null, null, null,
        "yyyy-01-01", Hourly, 0, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.nowMinusOffset(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[Africa/Cairo]"))

      val expected = ZonedDateTime.parse("2020-06-25T13:05:30+01:00[UTC]")
      actual shouldBe expected
    }

    "work with non-UTC when offset is not zero" in {
      val job = Job(1, null, null, null,
        "yyyy-01-01", Annually, 1, ZoneId.of("UTC"),
        3, null, Seq.empty
      )

      val actual =
        StatusChecker.nowMinusOffset(job, ZonedDateTime.parse("2020-06-25T15:05:30+01:00[Africa/Cairo]"))

      val expected = ZonedDateTime.parse("2019-06-25T13:05:30+01:00[UTC]")
      actual shouldBe expected
    }
  }

  "Refresh" must {
    "work" in {
      val now = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          farFuture, () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              tstamp,
              Vector(
                PeriodHealth("2020-06-25-13", false),
                PeriodHealth("2020-06-25-14", true))),
            JobStatus(
              job2,
              tstamp,
              Vector(
                PeriodHealth("2020-06-25-13", false))))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              tstamp,
              Vector(
                PeriodHealth("2020-06-25-12", false),
                PeriodHealth("2020-06-25-13", false),
                PeriodHealth("2020-06-25-14", true))),
            JobStatus(
              job4,
              tstamp,
              Vector(
                PeriodHealth("2020-06-25-11", false),
                PeriodHealth("2020-06-25-12", false),
                PeriodHealth("2020-06-25-13", true),
                PeriodHealth("2020-06-25-14", true))))))

      actor ! Refresh(() => now)

      eventually {
        actor ! AllEntries
        val msg = receiveOne(5 second)
        msg shouldBe expected
      }
    }

    "not do anything if refresh job is expired" in {
      val now = ZonedDateTime.now
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          // expire in the past
          (-1 * (System.currentTimeMillis + 10000)) / 1000,
          () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              -1,
              Vector.empty),
            JobStatus(
              job2,
              -1,
              Vector.empty))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job4,
              -1,
              Vector.empty,
              ))))

      actor ! Refresh(() => now)

      // Sleep 2 seconds, so the async call finishes
      Thread.sleep(2000)

      actor ! AllEntries
      val msg = receiveOne(5 second)
      msg shouldBe expected
    }
  }

  "RefreshGroup" must {
    "not do anything if refresh job is expired" in {
      val now = ZonedDateTime.now
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          // expire in the past
          (-1 * (System.currentTimeMillis + 10000)) / 1000,
          () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              -1,
              Vector.empty),
            JobStatus(
              job2,
              -1,
              Vector.empty))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job4,
              -1,
              Vector.empty,
              ))))

      actor ! RefreshGroup(() => now, 0)

      expectMsg(true)

      // Sleep 2 seconds, so the async call finishes
      Thread.sleep(2000)

      actor ! AllEntries
      val msg = receiveOne(5 second)
      msg shouldBe expected
    }

    "work and reply with true when group id exists" in {
      val now = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          farFuture, () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              tstamp,
              Vector(
                PeriodHealth("2020-06-25-13", false),
                PeriodHealth("2020-06-25-14", true))),
            JobStatus(
              job2,
              tstamp,
              Vector(
                PeriodHealth("2020-06-25-13", false))))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job4,
              -1,
              Vector.empty,
              ))))

      actor ! RefreshGroup(() => now, 0)

      expectMsg(true)

      eventually {
        actor ! AllEntries
        val msg = receiveOne(5 second)
        msg shouldBe expected
      }
    }

    "reply with false when group id does not exist" in {
      val now = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          farFuture, () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job2,
              -1,
              Vector.empty,
              ))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job4,
              -1,
              Vector.empty,
              ))))

      actor ! RefreshGroup(() => now, 10)

      expectMsg(false)

      actor ! AllEntries
      val msg = receiveOne(5 second)
      msg shouldBe expected
    }
  }

  "RefreshJob" must {

    "not do anything if refresh job is expired" in {
      val now = ZonedDateTime.now
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          // expire in the past
          (-1 * (System.currentTimeMillis + 10000)) / 1000,
          () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              -1,
              Vector.empty),
            JobStatus(
              job2,
              -1,
              Vector.empty))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job4,
              -1,
              Vector.empty,
              ))))

      actor ! RefreshJob(() => now, 0, 0)

      expectMsg(true)

      // Sleep 2 seconds, so the async call finishes
      Thread.sleep(2000)

      actor ! AllEntries
      val msg = receiveOne(5 second)
      msg shouldBe expected
    }

    "work and reply with true when group and job ids exist" in {
      val now = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          farFuture, () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              tstamp,
              Vector(
                PeriodHealth("2020-06-25-13", false),
                PeriodHealth("2020-06-25-14", true))),
            JobStatus(
              job2,
              -1,
              Vector.empty,
              ))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job4,
              -1,
              Vector.empty,
              ))))

      actor ! RefreshJob(() => now, 0, 0)

      expectMsg(true)

      eventually {
        actor ! AllEntries
        val msg = receiveOne(5 second)
        msg shouldBe expected
      }
    }

    "reply with false when group id does not exist" in {
      val now = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          farFuture, () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job2,
              -1,
              Vector.empty,
              ))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job4,
              -1,
              Vector.empty,
              ))))

      actor ! RefreshJob(() => now, 10, 0)

      expectMsg(false)

      actor ! AllEntries
      val msg = receiveOne(5 second)
      msg shouldBe expected
    }

    "reply with false when job id does not exist" in {
      val now = ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]")
      val actor = system.actorOf(Props(
        new StatusChecker(groups, stats,
          farFuture, () => tstamp)))

      val expected = List(
        GroupStatus(
          group1,
          Array(
            JobStatus(
              job1,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job2,
              -1,
              Vector.empty,
              ))),
        GroupStatus(
          group2,
          Array(
            JobStatus(
              job3,
              -1,
              Vector.empty,
              ),
            JobStatus(
              job4,
              -1,
              Vector.empty,
              ))))

      actor ! RefreshJob(() => now, 0, 10)

      expectMsg(false)

      actor ! AllEntries
      val msg = receiveOne(5 second)
      msg shouldBe expected
    }
  }
}
