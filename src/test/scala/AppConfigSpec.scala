package me.amanj.greenish

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import java.time.ZoneId
import models._

class AppConfigSpec() extends Matchers
  with AnyWordSpecLike {

  "AppConfig" must {
    "read config file correctly" in {
      val actual = AppConfig()
      val expected = new AppConfig(
        Seq(
          Group(0, "Group1", Seq(
              Job(0, "Job1", "job_1", "/tmp/first_script",
                "yyyy-MM-dd-HH", Hourly, 3,
                ZoneId.of("UTC"), 24,
                AlertLevels(0, 1, 2, 3)),
              Job(1, "Job2", "job_2", "/tmp/second_script job2",
                "yyyy-MM-dd-HH", Daily, 2,
                ZoneId.of("UTC"), 24,
                AlertLevels(0, 1, 2, 3)),
              Job(2, "Job5", "group1_job5", "/tmp/second_script job5",
                "yyyy-MM-dd-HH", Hourly, 2,
                ZoneId.of("US/Alaska"), 24,
                AlertLevels(0, 1, 2, 3)),
            )),
          Group(1, "Group2", Seq(
            Job(0, "Job3", "job_3", "/tmp/third_script",
                "yyyy-MM-dd", Monthly, 1,
                ZoneId.of("UTC"), 3,
                AlertLevels(0, 1, 2, 3)),
            Job(1, "Job4", "job_4", "/tmp/fourth_script",
                "yyyy-01-01", Annually, 1,
                ZoneId.of("UTC"), 3,
                AlertLevels(0, 1, 2, 3)),
            Job(2, "Job6", "group2_job6", "/tmp/second_script job6",
              "yyyy-MM-dd-HH-mm", Daily, 1,
              ZoneId.of("US/Samoa"), 270,
              AlertLevels(30, 40, 50, 60)),
            )),
        ),
        30,
        "127.0.0.1",
        8080,
        Seq("VAR1" -> "foo", "VAR2" -> "bar"),
      )
      actual shouldBe expected
    }
  }

  "toFrequency" must {
    import AppConfig.toFrequency
    "handle both lower and upper case frequencies" in {
      toFrequency("hOURly") shouldBe Hourly
      toFrequency("AnnuaLLy") shouldBe Annually
      toFrequency("monthly") shouldBe Monthly
      toFrequency("DAILY") shouldBe Daily
    }

    "throw an exception when it doesn't recognize a frequency" in {
      intercept[Exception](toFrequency("kkk"))
      intercept[Exception](toFrequency("weekly"))
      intercept[Exception](toFrequency("minutes"))
    }
  }

  "normalizePrometheusId" must {
    import AppConfig.normalizePrometheusId
    "convert prometheus_id to all lowercase" in {
      normalizePrometheusId("ABC") shouldBe "abc"
    }

    "replace whitesapce characters in prometheus_id to _" in {
      normalizePrometheusId("a b\nc\td\t") shouldBe "a_b_c_d_"
    }

    "throw exception when prometheus_id starts with a digit" in {
      intercept[Exception](normalizePrometheusId("9a b\nc\td\t"))
    }

    "throw exception when prometheus_id contains anything but [a-zA-Z0-9_]" in {
      intercept[Exception](normalizePrometheusId("a;a"))
    }

    "throw exception when prometheus_id is empty string" in {
      intercept[Exception](normalizePrometheusId(""))
    }
    "accept valid characters in the begining prometheus_id" in {
      normalizePrometheusId("a") shouldBe "a"
      normalizePrometheusId("A") shouldBe "a"
      normalizePrometheusId("_") shouldBe "_"
    }
  }

  "getIntWithDefault" must {
    import com.typesafe.config.ConfigFactory
    import AppConfig._
    val config = ConfigFactory.load()
    val appConfig = config.getConfig("check-groups")
    "get what the value of the property if the key exists" in {
      val actual = appConfig.getIntWithDefault("default-error-at", 100)
      val expected = 60
      actual shouldBe expected
    }

    "return default value if the key doesn't exists" in {
      val actual = appConfig.getIntWithDefault("naaah", 100)
      val expected = 100
      actual shouldBe expected
    }
  }

  "getStringWithDefault" must {
    import com.typesafe.config.ConfigFactory
    import AppConfig._
    val config = ConfigFactory.load()
    val appConfig = config.getConfig("check-groups")
    "get what the value of the property if the key exists" in {
      val actual = appConfig.getStringWithDefault("default-period-pattern", "kkkk")
      val expected = "yyyy-MM-dd-HH-mm"
      actual shouldBe expected
    }

    "return default value if the key doesn't exists" in {
      val actual = appConfig.getStringWithDefault("naaah", "kkkk")
      val expected = "kkkk"
      actual shouldBe expected
    }
  }
}


