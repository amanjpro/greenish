package me.amanj.greenish.checker

import me.amanj.greenish.models._
import java.time.ZonedDateTime
import akka.actor.Actor
import scala.sys.process.Process


class CommandRunner() extends Actor {
  override def receive: Receive = {
    case Run(cmd, env) =>
      val exitStatus = Process(cmd, None, env:_*).! == 0
      context.sender ! exitStatus
  }
}

