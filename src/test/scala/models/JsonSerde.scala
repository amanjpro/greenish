package me.amanj.greenish.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import java.time.ZoneId
import io.circe.Json
import io.circe.parser._
import io.circe.syntax.EncoderOps

class JsonSerde() extends Matchers
  with AnyWordSpecLike {
  "AlertLevel" must {
    "produce correct JSON" in {
      (Great: AlertLevel).asJson shouldBe "great".asJson
      (Normal: AlertLevel).asJson shouldBe "normal".asJson
      (Warn: AlertLevel).asJson shouldBe "warn".asJson
      (Critical: AlertLevel).asJson shouldBe "critical".asJson
    }

    "correctly parse JSON string" in {
      parse(""""great"""").flatMap(_.as[AlertLevel]).getOrElse(???) shouldBe Great
      parse(""""normal"""").flatMap(_.as[AlertLevel]).getOrElse(???) shouldBe Normal
      parse(""""warn"""").flatMap(_.as[AlertLevel]).getOrElse(???) shouldBe Warn
      parse(""""critical"""").flatMap(_.as[AlertLevel]).getOrElse(???) shouldBe Critical
    }
  }

  "AlertLevels" must {
    "produce correct JSON" in {
      val expected = Json.obj (
        "great" -> 1.asJson,
        "normal" -> 2.asJson,
        "warn" -> 3.asJson,
        "critical" -> 4.asJson,
      )
      val actual = AlertLevels(1, 2, 3, 4).asJson
      actual shouldBe expected
    }
  }

  "CheckFrequency" must {
    "produce correct JSON" in {
      (Hourly: CheckFrequency).asJson shouldBe "hourly".asJson
      (Daily: CheckFrequency).asJson shouldBe "daily".asJson
      (Monthly: CheckFrequency).asJson shouldBe "monthly".asJson
      (Annually: CheckFrequency).asJson shouldBe "annually".asJson
    }
  }

  "Group" must {
    "produce correct JSON" in {
      val job = Job(1, "j", "c", "yyyy-MM-dd",
        Hourly, ZoneId.of("UTC"), 2, AlertLevels(3, 4, 5, 6))
      val actual = Group(0, "g", Seq(job)).asJson

      val expected = Json.obj(
        "groupId" -> 0.asJson,
        "name" -> "g".asJson,
        "entries" -> Seq(job).asJson
      )

      actual shouldBe expected
    }
  }

  "GroupStatus" must {
    "produce correct JSON" in {
      val job = Job(1, "j", "c", "yyyy-MM-dd",
        Hourly, ZoneId.of("UTC"), 2, AlertLevels(3, 4, 5, 6))
      val group = Group(0, "g", Seq(job))
      val periods = Seq(PeriodHealth("1", true), PeriodHealth("2", false))
      val jobStatus = JobStatus(job, periods)
      val expected = Json.obj(
        "group" -> group.asJson,
        "status" -> Seq(jobStatus).asJson,
      )

      val actual = GroupStatus(group, Seq(jobStatus)).asJson
      actual shouldBe expected
    }
  }

  "GroupStatusSummary" must {
    "produce correct JSON" in {
      val jobStatus = Seq(JobStatusSummary("j", 1, Critical))

      val expected = Json.obj(
        "name" -> "g".asJson,
        "status" -> jobStatus.asJson,
      )

      val actual = GroupStatusSummary("g", jobStatus).asJson
      actual shouldBe expected
    }
  }

  "Job" must {
    "produce correct JSON" in {
      val alertLevels = AlertLevels(3, 4, 5, 6)
      val actual = Job(1, "j", "c", "yyyy-MM-dd",
        Hourly, ZoneId.of("UTC"), 2, alertLevels).asJson

      val expected = Json.obj(
        "jobId" -> 1.asJson,
        "name" -> "j".asJson,
        "cmd" -> "c".asJson,
        "timePattern" -> "yyyy-MM-dd".asJson,
        "frequency" -> "hourly".asJson,
        "timezone" -> Json.obj ("zoneId" -> "UTC".asJson),
        "lookback" -> 2.asJson,
        "alertLevels" -> alertLevels.asJson,
      )

      actual shouldBe expected
    }
  }

  "JobStatus" must {
    "produce correct JSON" in {
      val job = Job(1, "j", "c", "yyyy-MM-dd",
        Hourly, ZoneId.of("UTC"), 2, AlertLevels(3, 4, 5, 6))
      val periods = Seq(PeriodHealth("1", true), PeriodHealth("2", false))
      val expected = Json.obj(
        "job" -> job.asJson,
        "periodHealth" -> periods.asJson,
      )

      val actual = JobStatus(job, periods).asJson
      actual shouldBe expected
    }
  }

  "JobStatusSummary" must {
    "produce correct JSON" in {
      val expected = Json.obj(
        "name" -> "j".asJson,
        "missing" -> 1.asJson,
        "alertLevel" -> "critical".asJson,
      )

      val actual = JobStatusSummary("j", 1, Critical).asJson
      actual shouldBe expected
    }
  }

  "Lag" must {
    "produce correct JSON" in {
      val expected = Json.obj(
        "lag" -> 4.asJson,
      )

      val actual = Lag(4).asJson
      actual shouldBe expected
    }
  }

  "PeriodHealth" must {
    "produce correct JSON" in {
      val expected = Json.obj(
        "period" -> "2020-06-25-18".asJson,
        "ok" -> false.asJson,
      )

      val actual = PeriodHealth("2020-06-25-18", false).asJson
      actual shouldBe expected
    }
  }
}

