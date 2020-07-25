package me.amanj.greenish.checker

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import me.amanj.greenish.models.PeriodHealth

class CheckerSpec() extends AnyWordSpecLike with Matchers {

  "computeOldest" must {
    "work for empty period health lists" in {
      val periods = Seq.empty[PeriodHealth]
      val actual = computeOldest(periods)
      val expected = 0
      actual shouldBe expected
    }

    "work when the first period is missing" in {
      val periods = Seq(
        PeriodHealth("kaka", false),
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", true),
        )
      val actual = computeOldest(periods)
      val expected = 4
      actual shouldBe expected
    }

    "work when a middle period is missing" in {
      val periods = Seq(
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", false),
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", true),
        )
      val actual = computeOldest(periods)
      val expected = 3
      actual shouldBe expected
    }

    "work when the last period is missing" in {
      val periods = Seq(
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", false),
        )
      val actual = computeOldest(periods)
      val expected = 1
      actual shouldBe expected
    }

    "work when more than a period is missing" in {
      val periods = Seq(
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", false),
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", false),
        )
      val actual = computeOldest(periods)
      val expected = 3
      actual shouldBe expected
    }

    "work when no period is missing" in {
      val periods = Seq(
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", true),
        PeriodHealth("kaka", true),
        )
      val actual = computeOldest(periods)
      val expected = 0
      actual shouldBe expected
    }
  }
}


