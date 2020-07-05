package me.amanj.greenish.checker

import me.amanj.greenish.models._
import java.time.ZonedDateTime
import akka.actor.Actor
import scala.sys.process.Process
import scala.util.control.NonFatal
import akka.actor.ActorLogging


class CommandRunner() extends Actor with ActorLogging {
  override def receive: Receive = {
    case BatchRun(cmd, periods, env, group, job, clockCounter) =>
      try {
        val output =
          Process(s"$cmd ${periods.mkString(" ")}", None, env:_*).lazyLines_!
        val Matcher = "^greenish-period\t(.*)\t((1|0))$".r
        val capturedOutput = output.map { line =>
          line match {
            case Matcher(period, "1") => Some((period, true))
            case Matcher(period, "0") => Some((period, false))
            case _                   => None
          }
        }.collect { case Some(periodStatus) => periodStatus }
         .filter { case (period, _) => periods.contains(period) }
         .toSeq

       val distinctReturnedPeriods = capturedOutput.map(_._1).distinct
       if(capturedOutput.length != periods.size) {
         log.error(s"""|Some periods weren't returned for:
                       |Group ID: $group, Job ID: $job
                       |$cmd $periods
                       |state update aborted""".stripMargin)
       } else if(distinctReturnedPeriods.length != periods.size) {
         log.error(s"""|Some periods were returned more than once for:
                       |$cmd $periods
                       |Group ID: $group, Job ID: $job
                       |$cmd $periods
                       |state update aborted""".stripMargin)
       } else context.sender ! RunResult(capturedOutput.toMap, group, job, clockCounter)
      } catch {
        case NonFatal(exp) =>
          log.error(exp.getMessage())
          context.sender ! periods.map((_, false))
      }
    case Run(cmd, env) =>
      try {
        val exitStatus = Process(Seq("bash", "-c", cmd), None, env:_*).! == 0
        context.sender ! exitStatus
      } catch {
        case NonFatal(exp) =>
          log.error(exp.getMessage())
          context.sender ! false
      }
  }
}

