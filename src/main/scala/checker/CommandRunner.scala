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
        val periodsSet = periods.toSet
        val exec = Seq("bash", "-c", s"$cmd ${periods.mkString(" ")}")
        val output = Process(exec, None, env:_*).lazyLines
        val Matcher = "^greenish-period\t(.*)\t(1|0)$".r
        val capturedOutput = output.map { line =>
          line match {
            case Matcher(period, "1") => Some((period, true))
            case Matcher(period, "0") => Some((period, false))
            case _                   => None
          }
        }.collect { case Some(periodStatus) => periodStatus }
         .filter { case (period, _) => periodsSet.contains(period) }

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
       } else {
         val mapped = capturedOutput.toMap
         val periodHealths = periods.map { period => PeriodHealth(period, mapped(period)) }
         context.sender ! RunResult(periodHealths, group, job, clockCounter)
       }
      } catch {
        case NonFatal(exp) =>
          log.error(exp.getMessage())
      }
  }
}
