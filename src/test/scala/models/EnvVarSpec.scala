package me.amanj.greenish.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class EnvVarSpec() extends Matchers with AnyWordSpecLike {
  "EnvVar.apply" must {
    "create PlainEnvVar when secure flag is not provided" in {
      val (name, value) = ("username", "Homa")
      val expected = PlainEnvVar(name, value)
      val actual = EnvVar(name, value)
      actual shouldBe expected
    }

    "create SecureEnvVar when secure flag is provided" in {
      val (name, value) = ("username", "secure(Homa)")
      val expected = SecureEnvVar(name, "Homa".toSeq)
      val actual = EnvVar(name, value)
      actual shouldBe expected
    }

    "create SecureEnvVar when secure flag is provided but value is empty" in {
      val (name, value) = ("username", "secure()")
      val expected = SecureEnvVar(name, "".toSeq)
      val actual = EnvVar(name, value)
      actual shouldBe expected
    }
  }

  "EnvVar.tupled" must {
    "work for secure variables" in {
      val (name, value) = ("username", "secure(Homa)")
      val origin = EnvVar(name, value)
      val expected = (name, "Homa")
      val actual = origin.tupled
      actual shouldBe expected
    }

    "work for plain variables" in {
      val (name, value) = ("username", "Homa")
      val origin = EnvVar(name, value)
      val expected = (name, value)
      val actual = origin.tupled
      actual shouldBe expected
    }
  }
}

