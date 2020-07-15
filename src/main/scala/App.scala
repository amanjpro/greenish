package me.amanj.greenish

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import scala.language.postfixOps
import akka.stream.ActorMaterializer
import scala.concurrent.duration._
import java.time.ZonedDateTime
import checker.{StatusChecker, Refresh}
import endpoints.Routes

object App {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("greenish-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher
    val schedulerActor = system.actorOf(Props.empty)

    val appConfig = AppConfig()

    val prometheusIds = appConfig.groups.flatMap ( g =>
      g.jobs.map(j => j.prometheusId)).toSet

    val statsActor = system.actorOf(
      Props(new stats.StatsCollector(prometheusIds)))

    val statusChecker = system.actorOf(
      Props(new StatusChecker(appConfig.groups, statsActor, appConfig.env)))

    system.scheduler.scheduleWithFixedDelay(
      0 seconds,
      appConfig.refreshInSeconds seconds,
      statusChecker, Refresh(() => ZonedDateTime.now()))

    val bindingFuture = Http().bindAndHandle(new Routes(statusChecker).routes,
      appConfig.address, appConfig.port)

    println(s"Server online at http://${appConfig.address}:${appConfig.port}...")
  }
}
