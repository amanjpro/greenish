package me.amanj.greenish.checker

import java.time.ZonedDateTime

sealed trait Message
case class Refresh(now: () => ZonedDateTime) extends Message
case object MaxLag extends Message
case object AllEntries extends Message
case object GetMissing extends Message
case object Summary extends Message

