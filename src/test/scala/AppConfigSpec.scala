package me.amanj.greenish

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import java.time.ZoneId
import models._

class CommandRunnerSpec() extends Matchers
  with AnyWordSpecLike {

  "AppConfig" must {
    "read config file correctly" in {
      val actual = AppConfig()
      val expected = new AppConfig(
        Seq(
          Group(0, "Group1", Seq(
              Job(0, "Job1", "/tmp/first_script",
                "yyyy-MM-dd-HH", Hourly,
                ZoneId.of("UTC"), 24,
                AlertLevels(0, 1, 2, 3)),
              Job(1, "Job2", "/tmp/second_script job2",
                "yyyy-MM-dd-HH", Daily,
                ZoneId.of("UTC"), 24,
                AlertLevels(0, 1, 2, 3)),
            )),
          Group(1, "Group2", Seq(
            Job(0, "Job3", "/tmp/third_script",
                "yyyy-MM-dd", Monthly,
                ZoneId.of("UTC"), 3,
                AlertLevels(0, 1, 2, 3)),
            Job(1, "Job4", "/tmp/fourth_script",
                "yyyy-01-01", Annually,
                ZoneId.of("UTC"), 3,
                AlertLevels(0, 1, 2, 3)),
            )),
        ),
        30,
        8080,
        Seq("VAR1" -> "foo", "VAR2" -> "bar"),
      )
      actual shouldBe expected
    }
  }
}


