package me.amanj.greenish

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import scala.language.postfixOps
import akka.stream.ActorMaterializer
import scala.concurrent.duration._
import scala.io.StdIn
import java.time.ZonedDateTime
import checker.{StatusChecker, Refresh}
import endpoints.Routes

object App {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("greenish-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher
    implicit val schedulerActor = system.actorOf(Props.empty)

    val appConfig = AppConfig()
    val statusChecker = system.actorOf(
      Props(new StatusChecker(appConfig.groups, appConfig.env,
        ZonedDateTime.now().minusHours(2))))

    system.scheduler.scheduleWithFixedDelay(
      appConfig.refreshInSeconds seconds,
      appConfig.refreshInSeconds seconds,
      statusChecker, Refresh(() => ZonedDateTime.now()))

    val bindingFuture = Http().bindAndHandle(new Routes(statusChecker).routes,
      "0.0.0.0", appConfig.port)

    println(s"Server online at http://localhost:${appConfig.port}...")
  }
}
