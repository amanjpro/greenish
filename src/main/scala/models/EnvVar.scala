package me.amanj.greenish.models

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Decoder, HCursor, Json}

sealed trait EnvVar {
  type T <: AnyRef
  def name: String
  def value: T
}
object EnvVar {
  private[this] val pattern = """secure\((.*)\)""".r
  def apply(key: String, value: String): EnvVar = {
    value match {
      case pattern(v) => SecureEnvVar(key, v.toSeq)
      case _ => PlainEnvVar(key, value)
    }
  }

  implicit val envVarDecoer: Decoder[EnvVar] = new Decoder[EnvVar] {
    final def apply(obj: HCursor): Decoder.Result[EnvVar] = {
      println(obj.downField("type"))
      obj.downField("type").as[String].flatMap {
        case "secure" => obj.as[SecureEnvVar]
        case "plain" => obj.as[PlainEnvVar]
      }
    }
  }

  implicit val envVarEncoder: Encoder[EnvVar] = Encoder.instance {
    case sec: SecureEnvVar         => sec.asJson
    case plain: PlainEnvVar        => plain.asJson
  }
}

case class SecureEnvVar private(name: String, value: Seq[Char]) extends EnvVar {
  type T = Seq[Char]
}

object SecureEnvVar {
  val HIDDEN_PASSWORD = "****"
  implicit val secureEnvVarEncoder: Encoder[SecureEnvVar] =
    new Encoder[SecureEnvVar] {
      final def apply(v: SecureEnvVar): Json = Json.obj(
        ("type", Json.fromString("secure")),
        ("name", Json.fromString(v.name)),
        ("value", Json.fromString(HIDDEN_PASSWORD)),
      )
    }

  implicit val secureEnvVarDecoder: Decoder[SecureEnvVar] = new Decoder[SecureEnvVar] {
    final def apply(c: HCursor): Decoder.Result[SecureEnvVar] =
      c.downField("type").as[String].flatMap {
        case "secure" =>
          for {
            name <- c.downField("name").as[String]
            value <- c.downField("value").as[String].map(_.toSeq)
          } yield SecureEnvVar(name, value)
      }
    }
}

case class PlainEnvVar private(name: String, value: String) extends EnvVar {
  type T = String
}
object PlainEnvVar {
  implicit val plainEnvVarEncoder: Encoder[PlainEnvVar] =
    new Encoder[PlainEnvVar] {
      final def apply(v: PlainEnvVar): Json = Json.obj(
        ("type", Json.fromString("plain")),
        ("name", Json.fromString(v.name)),
        ("value", Json.fromString(v.value)),
      )
    }

  implicit val secureEnvVarDecoder: Decoder[PlainEnvVar] = new Decoder[PlainEnvVar] {
    final def apply(c: HCursor): Decoder.Result[PlainEnvVar] =
      c.downField("type").as[String].flatMap {
        case "plain" =>
          for {
            name <- c.downField("name").as[String]
            value <- c.downField("value").as[String]
          } yield PlainEnvVar(name, value)
      }
    }
}
