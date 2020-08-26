package me.amanj.greenish.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import java.time.ZoneId
import io.circe.Json
import io.circe.parser._
import io.circe.syntax.EncoderOps

class JsonSerdeSpec() extends Matchers
  with AnyWordSpecLike {
  "healthJson" must {
    "produce correct JSON when health is bad" in {
      val expected = "bad"
      val json = healthJson(false)
      val actual = json.hcursor.downField("health").as[String].getOrElse(???)
      actual shouldBe expected
      json.hcursor.keys.get.size shouldBe 1
    }

    "produce correct JSON when health is good" in {
      val expected = "good"
      val json = healthJson(true)
      val actual = json.hcursor.downField("health").as[String].getOrElse(???)
      actual shouldBe expected
      json.hcursor.keys.get.size shouldBe 1
    }
  }

  "errorJson" must {
    "produce correct JSON" in {
      val expected = "Error"
      val json = errorJson(expected)
      val actual = json.hcursor.downField("error").as[String].getOrElse(???)
      actual shouldBe expected
      json.hcursor.keys.get.size shouldBe 1
    }
  }

  "okJson" must {
    "produce correct JSON" in {
      val expected = "OK"
      val json = okJson(expected)
      val actual = json.hcursor.downField("ok").as[String].getOrElse(???)
      actual shouldBe expected
      json.hcursor.keys.get.size shouldBe 1
    }
  }

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

    "correctly parse JSON" in {
      val expected = AlertLevels(1, 2, 3, 4)
      val actual = expected.asJson.as[AlertLevels].getOrElse(???)

      actual shouldBe expected
    }
  }

  "CheckFrequency" must {
    "produce correct JSON" in {
      (Hourly: CheckFrequency).asJson shouldBe "hourly".asJson
      (Daily: CheckFrequency).asJson shouldBe "daily".asJson
      (Monthly: CheckFrequency).asJson shouldBe "monthly".asJson
      (Annually: CheckFrequency).asJson shouldBe "annually".asJson
      val pattern = "* * * * *"
      val expected = Json.obj("pattern" -> pattern.asJson)
      Cron(pattern).asJson shouldBe expected
      (Cron(pattern): CheckFrequency).asJson shouldBe expected
    }

    "correctly parse JSON string" in {
      parse(""""hourly"""").flatMap(_.as[CheckFrequency]).getOrElse(???) shouldBe Hourly
      parse(""""daily"""").flatMap(_.as[CheckFrequency]).getOrElse(???) shouldBe Daily
      parse(""""monthly"""").flatMap(_.as[CheckFrequency]).getOrElse(???) shouldBe Monthly
      parse(""""annually"""").flatMap(_.as[CheckFrequency]).getOrElse(???) shouldBe Annually
      val pattern = "* * * * *"
      val expected = Cron(pattern)
      val actualCron = expected.asJson.as[Cron].getOrElse(???)
      actualCron shouldBe expected
      val actualCheckFrequency = expected.asJson.as[CheckFrequency].getOrElse(???)
      actualCheckFrequency shouldBe expected
    }
  }

  "EnvVar" must {
    "produce correct JSON it is PlainEnvVar" in {
      val actual = EnvVar("username", "Homa").asJson
      val expected = Json.obj (
        "type" -> "plain".asJson,
        "name" -> "username".asJson,
        "value" -> "Homa".asJson,
      )
      actual shouldBe expected
    }

    "correctly parse JSON string for PlainEnvVar" in {
      val expected = EnvVar("username", "Homa")
      val actual = expected.asJson.as[EnvVar].getOrElse(???)
      actual shouldBe expected
    }

    "produce correct JSO it is SecureEnvVar" in {
      val actual = EnvVar("username", "secure(Homa)").asJson
      val expected = Json.obj (
        "type" -> "secure".asJson,
        "name" -> "username".asJson,
        "value" -> SecureEnvVar.HIDDEN_PASSWORD.asJson,
      )
      actual shouldBe expected
    }

    "correctly parse JSON string for SecureEnvVar" in {
      val origin = EnvVar("username", "secure(Homa)")
      val expected = EnvVar("username", s"secure(${SecureEnvVar.HIDDEN_PASSWORD})")
      val actual = origin.asJson.as[EnvVar].getOrElse(???)
      actual shouldBe expected
    }
  }

  "Group" must {
    val job = Job(1, "j", None, "p", "c", "yyyy-MM-dd",
      Hourly, 1, ZoneId.of("UTC"), 2, 0, AlertLevels(3, 4, 5, 6),
      None, Seq("a" -> "b"))
    val group = Group(0, "g", Seq(job))

    "produce correct JSON" in {
      val actual = group.asJson

      val expected = Json.obj(
        "group_id" -> 0.asJson,
        "name" -> "g".asJson,
        "jobs" -> Seq(job).asJson
      )

      actual shouldBe expected
    }

    "correctly parse JSON" in {
      val expected = group
      val actual = expected.asJson.as[Group].getOrElse(???)

      actual shouldBe expected
    }
  }

  "GroupStatus" must {
    val job = Job(1, "j", None, "p", "c", "yyyy-MM-dd",
      Hourly, 1, ZoneId.of("UTC"), 2, 0, AlertLevels(3, 4, 5, 6),
      None, Seq("a" -> "b"))
    val group = Group(0, "g", Seq(job))
    val periods = Seq(PeriodHealth("1", true), PeriodHealth("2", false))
    val jobStatus = JobStatus(job, 100, periods)
    val groupStatus = GroupStatus(group, Array(jobStatus))

    "produce correct JSON" in {
            val expected = Json.obj(
        "group" -> group.asJson,
        "status" -> Seq(jobStatus).asJson,
      )

      val actual = groupStatus.asJson
      actual shouldBe expected
    }

    "correctly parse JSON" in {
      val expected = groupStatus
      val actual = expected.asJson.as[GroupStatus].getOrElse(???)

      actual shouldBe expected
    }
  }

  "GroupStatusSummary" must {
    val jobStatus = Seq(JobStatusSummary(0, "j", 1, 1, Critical))
    val groupStatusSummary = GroupStatusSummary(2, "g", jobStatus)
    "produce correct JSON" in {

      val expected = Json.obj(
        "group_id" -> 2.asJson,
        "name" -> "g".asJson,
        "status" -> jobStatus.asJson,
      )

      val actual = groupStatusSummary.asJson
      actual shouldBe expected
    }

    "correctly parse JSON" in {
      val expected = groupStatusSummary
      val actual = expected.asJson.as[GroupStatusSummary].getOrElse(???)

      actual shouldBe expected
    }
  }

  "Job" must {
    val alertLevels = AlertLevels(3, 4, 5, 6)
    val job = Job(1, "j", None, "p", "c", "yyyy-MM-dd",
      Hourly, 1, ZoneId.of("UTC"), 2, 0, alertLevels,
      None, Seq("a" -> "b"))

    "produce correct JSON when there is no owner and no info" in {
      val alertLevels = AlertLevels(3, 4, 5, 6)
      val actual = job.asJson

      val expected = Json.obj(
        "job_id" -> 1.asJson,
        "name" -> "j".asJson,
        "owner" -> Json.Null,
        "prometheus_id" -> "p".asJson,
        "cmd" -> "c".asJson,
        "time_pattern" -> "yyyy-MM-dd".asJson,
        "frequency" -> "hourly".asJson,
        "period_check_offset" -> 1.asJson,
        "timezone" -> Json.obj ("zone_id" -> "UTC".asJson),
        "lookback" -> 2.asJson,
        "start_at" -> 0.asJson,
        "alert_levels" -> alertLevels.asJson,
        "info" -> Json.Null,
        "env" -> Seq("a" -> "b").asJson,
      )

      actual shouldBe expected
    }

    "produce correct JSON when owner exists" in {
      val alertLevels = AlertLevels(3, 4, 5, 6)
      val actual = job.copy(owner=Some("me"), info=Some("you")).asJson

      val expected = Json.obj(
        "job_id" -> 1.asJson,
        "name" -> "j".asJson,
        "owner" -> "me".asJson,
        "prometheus_id" -> "p".asJson,
        "cmd" -> "c".asJson,
        "time_pattern" -> "yyyy-MM-dd".asJson,
        "frequency" -> "hourly".asJson,
        "period_check_offset" -> 1.asJson,
        "timezone" -> Json.obj ("zone_id" -> "UTC".asJson),
        "lookback" -> 2.asJson,
        "start_at" -> 0.asJson,
        "alert_levels" -> alertLevels.asJson,
        "info" -> "you".asJson,
        "env" -> Seq("a" -> "b").asJson,
      )

      actual shouldBe expected
    }

    "correctly parse JSON" in {
      val expected = job
      val actual = expected.asJson.as[Job].getOrElse(???)

      actual shouldBe expected
    }
  }

  "JobStatus" must {
    val job = Job(1, "j", None, "p", "c", "yyyy-MM-dd",
      Hourly, 1, ZoneId.of("UTC"), 2, 0, AlertLevels(3, 4, 5, 6),
      None, Seq("a" -> "b")
      )
    val periods = Seq(PeriodHealth("1", true), PeriodHealth("2", false))
    val jobStatus = JobStatus(job, 100, periods)

    "produce correct JSON" in {
      val expected = Json.obj(
        "job" -> job.asJson,
        "updated_at" -> 100.asJson,
        "period_health" -> periods.asJson,
      )

      val actual = jobStatus.asJson
      actual shouldBe expected
    }

    "correctly parse JSON" in {
      val expected = jobStatus
      val actual = expected.asJson.as[JobStatus].getOrElse(???)

      actual shouldBe expected
    }
  }

  "JobStatusSummary" must {
    val jobStatusSummary = JobStatusSummary(0, "j", 1, 2, Critical)
    "produce correct JSON" in {
      val expected = Json.obj(
        "job_id" -> 0.asJson,
        "name" -> "j".asJson,
        "missing" -> 1.asJson,
        "oldest_missing_period" -> 2.asJson,
        "alert_level" -> "critical".asJson,
      )

      val actual = jobStatusSummary.asJson
      actual shouldBe expected
    }

    "correctly parse JSON" in {
      val expected = jobStatusSummary
      val actual = expected.asJson.as[JobStatusSummary].getOrElse(???)

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

    "correctly parse JSON" in {
      val expected = Lag(5)
      val actual = expected.asJson.as[Lag].getOrElse(???)

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

    "correctly parse JSON" in {
      val expected = PeriodHealth("2020-06-25-18", false)
      val actual = expected.asJson.as[PeriodHealth].getOrElse(???)

      actual shouldBe expected
    }
  }

  "sysinfo" must {
    "produce correct JSON when namespace is missing" in {
      val json = sysinfo(None)
      val cursor = json.hcursor
      cursor.downField("version").as[Option[String]].isRight shouldBe true
      cursor.downField("namespace").as[Option[String]] shouldBe Right(None)
      cursor.downField("service").as[String] shouldBe Right("Greenish")
      cursor.downField("uptime").as[Long].isRight shouldBe true
      cursor.keys.get.size shouldBe 4
    }

    "produce correct JSON when namespace is not missing" in {
      val json = sysinfo(Some("my dashboard"))
      val cursor = json.hcursor
      cursor.downField("version").as[Option[String]].isRight shouldBe true
      cursor.downField("namespace").as[Option[String]] shouldBe Right(Some("my dashboard"))
      cursor.downField("service").as[String] shouldBe Right("Greenish")
      cursor.downField("uptime").as[Long].isRight shouldBe true
      cursor.keys.get.size shouldBe 4
    }
  }
}

