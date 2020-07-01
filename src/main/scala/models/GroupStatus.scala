package me.amanj.greenish.models

import io.circe.{Encoder, Decoder}
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}

case class GroupStatus(
  group: Group,
  status: Array[JobStatus],
) {
  def canEqual(a: Any) = a.isInstanceOf[GroupStatus]

  override def equals(that: Any): Boolean =
    that match {
      case that: GroupStatus => {
        that.canEqual(this) &&
        this.group == that.group &&
        this.status.sameElements(that.status)
      }
      case _ => false
    }

  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + group.hashCode;
    result = prime * result + (if (group == null) 0 else group.hashCode)
    result = prime * result + (if (status == null) 0 else status.toVector.hashCode)
    result
  }

  override def toString: String = {
    s"Group($group, ${status.mkString("Array(", ", ", ")")})"
  }
}

object GroupStatus {
  implicit val groupStatusDecoder: Decoder[GroupStatus] = deriveConfiguredDecoder
  implicit val groupStatusEncoder: Encoder[GroupStatus] = deriveConfiguredEncoder
}
