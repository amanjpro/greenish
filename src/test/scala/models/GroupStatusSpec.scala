package me.amanj.greenish.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import java.time.ZoneId

class GroupStatusSpec() extends Matchers
  with AnyWordSpecLike {

  val job1 = Job(1, "job1", None, "p1", "foo",
      "yyyy-MM-dd-HH", Hourly, 1, ZoneId.of("UTC"),
      4, 0, AlertLevels(0, 1, 2, 3), None, Seq(EnvVar("a", "b"))
    )

  val job2 = Job(2, "job2", None, "p2", "bar",
      "yyyy-MM-dd-HH", Hourly, 1, ZoneId.of("UTC"),
      4, 0, AlertLevels(0, 1, 2, 3), None, Seq(EnvVar("a", "secure(b)"))
    )

  val group1 = Group(0, "group1", Seq(job1))
  val group2 = Group(1, "group2", Seq(job2))

  val gs1 = GroupStatus(group1, Array(JobStatus(job1, -1, Seq.empty)))
  val gs1Copy = GroupStatus(group1, Array(JobStatus(job1, -1, Seq.empty)))
  val gs2 = GroupStatus(group2, Array(JobStatus(job2, -1, Seq.empty)))

  "equals" must {
    "work if that is null" in {
      val actual = gs1 == null
      actual shouldBe false
    }

    "work if that is this" in {
      val actual = gs1 == gs1
      actual shouldBe true
    }

    "work if that is a clone of this" in {
      val actual = gs1 == gs1Copy
      actual shouldBe true
    }

    "not be equal to non-GroupStatus objects" in {
      val actual = gs1 == job1
      actual shouldBe false
    }
  }

  "hashCode" must {
    "be predictive" in {
      val actual = gs1.## == gs1.##
      actual shouldBe true
    }

    "produce the same value for equivalent objects" in {
      val actual = gs1.## == gs1Copy.##
      actual shouldBe true
    }

    "produce differe values for different objects" in {
      val actual = gs1.## == gs2.##
      actual shouldBe false
    }
  }
}

