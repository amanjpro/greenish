package me.amanj.greenish.alerts

import me.amanj.greenish.models.{JobStatus, AlertLevel, Normal, Critical}
import java.io.File
import me.amanj.greenish.checker.{MaybeAlert, debugFile}
import scala.collection.{mutable => m}
import akka.actor.{Actor, ActorRef, ActorLogging}
import scala.io.Source
import java.util.UUID

class AlertActor(clients: Map[AlertLevel, AlertClient], outputDir: File)
  extends Actor with ActorLogging {

  private[this] val instanceId = UUID.randomUUID()
  private[this] val jobAlerts: m.Map[(Int, Int), AlertLevel] = m.Map.empty

  override def receive: Actor.Receive = {
    case MaybeAlert(groupId, groupName, jobId, jobName, alertLevel) =>
      val mapKey = (groupId, jobId)
      val maybeAlerted = jobAlerts.get(mapKey)
      val shouldAlert = maybeAlerted.map(_ == alertLevel).getOrElse(true)
      if(shouldAlert) {
        try {
          val stdout = Source.fromFile(debugFile(outputDir, groupId,
            jobId)).getLines.toSeq
          val info = JobInfo(s"group-$groupId-job-${jobId}-$instanceId",
            groupName, jobName, stdout)

          clients.get(alertLevel).foreach { client =>
            alertLevel match {
              case Critical | Normal => client.resolve(info)
              case _                 => client.alert(info, alertLevel)
            }
          }
          jobAlerts += ((mapKey, alertLevel))
        } catch {
          case e: Exception => log.error(e.getMessage)
        }
      }
  }
}
