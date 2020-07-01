package me.amanj.greenish.checker

import me.amanj.greenish.models._
import java.time.ZonedDateTime
import akka.actor.Actor
import scala.sys.process.Process
import scala.util.control.NonFatal
import akka.actor.ActorLogging


class CommandRunner() extends Actor with ActorLogging {
  override def receive: Receive = {
    case Run(cmd, env) =>
      try {
        val exitStatus = Process(cmd, None, env:_*).! == 0
        context.sender ! exitStatus
      } catch {
        case NonFatal(exp) =>
          log.error(exp.getMessage())
          context.sender ! false
      }
  }
}

