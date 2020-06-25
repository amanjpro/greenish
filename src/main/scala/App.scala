package me.amanj.greenish

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import scala.language.postfixOps
import akka.util.Timeout
import akka.http.scaladsl.model._
import akka.pattern.ask
import java.time.format.DateTimeFormatter
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.concurrent.duration._
import scala.io.StdIn
import java.time.ZonedDateTime
import checker._
import models.{CheckEntry, CheckGroup, AlertLevels}
import scala.concurrent.Future
import scala.util.{Success, Failure}
import java.time.ZoneId
import io.circe.syntax._
import io.circe.Printer

object App {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("greenish-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher
    implicit val schedulerActor = system.actorOf(Props.empty)
    implicit val timeout = Timeout(Duration.fromNanos(5000000000L))

    val checkEntities = List (
      CheckGroup(1, "Trader Delivery",
        Seq(
          CheckEntry (
            1,
            "Delivery Raw",
            "/usr/local/Homebrew/Library/Taps/dflemstr/homebrew-tools/check-delivery-raw",
            "yyyy-MM-dd-HH",
            models.Hourly,
            ZoneId.of("UTC"),
            24,
            AlertLevels(0, 1, 2, 3),
          ),
        )
      ),
      CheckGroup(2, "Trader Gateway",
        Seq(
          CheckEntry (
            1,
            "Gateway Raw",
            "/usr/local/Homebrew/Library/Taps/dflemstr/homebrew-tools/check-delivery-raw",
            "yyyy-MM-dd-HH",
            models.Hourly,
            ZoneId.systemDefault,
            3,
            AlertLevels(0, 1, 2, 3),
          )
        )
      )
    )

    val statusChecker = system.actorOf(
      Props(new StatusChecker(checkEntities, ZonedDateTime.now().minusHours(2))))

    system.scheduler.scheduleWithFixedDelay(30 seconds, 30 seconds,
      statusChecker, Refresh(() => ZonedDateTime.now()))

    val jsonPrinter = Printer (
      dropNullValues = true,
      indent=""
    )
    val route =
      concat(
        get {
          pathPrefix("maxlag") {
            val lagFuture = (
              statusChecker ? MaxLag
            ).mapTo[Int]
            onComplete(lagFuture) { lag =>
              complete(lag.map(o => jsonPrinter.print(o.asJson)))
            }
          }
        },
        get {
          path("missing") {
            val missingFuture = (
              statusChecker ? GetMissing
            ).mapTo[Seq[GroupStatus]]
            onComplete(missingFuture) { missing =>
              complete(missing.map(o => jsonPrinter.print(o.asJson)))
            }
          }
        },
        get {
          path("dashboard") {
            val allFuture = (
              statusChecker ? AllEntries
            ).mapTo[Seq[GroupStatus]]
            onComplete(allFuture) { completed =>
              complete(completed.map(o => jsonPrinter.print(o.asJson)))
                // groups.map(g => jsonPrinter.print(g.asJson)).mkString("[", ",", "]")
              // })
            }
          }
        }
      )

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
