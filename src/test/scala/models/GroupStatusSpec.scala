package me.amanj.greenish.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import java.time.ZoneId

class GroupStatusSpec() extends Matchers
  with AnyWordSpecLike {

  val job1 = Job(1, "job1", "foo",
      "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
      4, AlertLevels(0, 1, 2, 3),
    )

  val job2 = Job(2, "job2", "bar",
      "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
      4, AlertLevels(0, 1, 2, 3),
    )

  val group1 = Group(0, "group1", Seq(job1))
  val group2 = Group(1, "group2", Seq(job2))

  val gs1 = GroupStatus(group1, Array(JobStatus(job1, -1, Seq.empty)))
  val gs1Copy = GroupStatus(group1, Array(JobStatus(job1, -1, Seq.empty)))
  val gs2 = GroupStatus(group2, Array(JobStatus(job2, -1, Seq.empty)))

  "equals" must {
    "must work if that is null" in {
      val actual = gs1 == null
      actual shouldBe false
    }

    "must work if that is this" in {
      val actual = gs1 == gs1
      actual shouldBe true
    }

    "must work if that is a clone of this" in {
      val actual = gs1 == gs1Copy
      actual shouldBe true
    }
  }

  "hashCode" must {
    "must be predictive" in {
      val actual = gs1.## == gs1.##
      actual shouldBe true
    }

    "must produce the same value for equivalent objects" in {
      val actual = gs1.## == gs1Copy.##
      actual shouldBe true
    }

    "must produce differe values for different objects" in {
      val actual = gs1.## == gs2.##
      actual shouldBe false
    }
  }
}

