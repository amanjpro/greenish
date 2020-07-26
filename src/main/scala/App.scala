package me.amanj.greenish

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import scala.language.postfixOps
import akka.stream.ActorMaterializer
import scala.concurrent.duration._
import java.time.ZonedDateTime
import checker.{StatusChecker, Refresh}
import endpoints.Routes
import akka.event.{Logging, LogSource}

object App {

  private[this] implicit val system = ActorSystem("greenish-system")
  private[this] implicit val executionContext = system.dispatcher
  private[this] val schedulerActor = system.actorOf(Props.empty)

  implicit val logSource: LogSource[AnyRef] = new LogSource[AnyRef] {
    def genString(o: AnyRef): String = o.getClass.getName
    override def getClazz(o: AnyRef): Class[_] = o.getClass
  }

  private[this] val logger = Logging(system, this)

  def main(args: Array[String]): Unit = {

    val appConfig = AppConfig()

    val statsActor = system.actorOf(
      Props(new stats.StatsCollector(getPrometheusIds(appConfig))))

    val statusChecker = system.actorOf(
      Props(new StatusChecker(appConfig.groups, statsActor)))

    system.scheduler.scheduleWithFixedDelay(
      0 seconds,
      appConfig.refreshInSeconds seconds,
      statusChecker, Refresh(() => ZonedDateTime.now()))

    val bindingFuture = Http()
      .bindAndHandle(
        new Routes(appConfig.namespace, statusChecker,
          statsActor,
          // At least there should be one good run in the last 5 refresh sets
          appConfig.refreshInSeconds * 1000 * 5).routes,
        appConfig.address, appConfig.port)

    println(s"Server online at http://${appConfig.address}:${appConfig.port}...")
  }

  def getPrometheusIds(appConfig: AppConfig): Set[String] = {
    val prometheusIds = appConfig.groups.flatMap ( g =>
      g.jobs.map(j => j.prometheusId))

    val prometheusIdsSet = prometheusIds.toSet
    if(prometheusIdsSet.size < prometheusIds.size) {
      logger.warning(
        "prometheus-id is best to be unique per the entire configuration")
    }

    prometheusIdsSet
  }


}
