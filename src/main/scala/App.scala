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
import models.CheckEntry
import scala.concurrent.Future
import scala.util.{Success, Failure}
import java.time.ZoneId

object App {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("greenish-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher
    implicit val schedulerActor = system.actorOf(Props.empty)
    implicit val timeout = Timeout(Duration.fromNanos(5000000000L))

    val checkEntities = List (
      CheckEntry (
        1,
        CheckGroup(1, "Trader Delivery"),
        "Delivery Raw",
        "/usr/local/Homebrew/Library/Taps/dflemstr/homebrew-tools/check-delivery-raw",
        DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"),
        models.Hourly,
        ZoneId.of("UTC"),
        24,
        AlertLevels(0, 1, 2, 3),
      ),
      CheckEntry (
        1,
        CheckGroup(1, "Trader Gateway"),
        "Gateway Raw",
        "/usr/local/Homebrew/Library/Taps/dflemstr/homebrew-tools/check-delivery-raw",
        DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"),
        models.Hourly,
        ZoneId.systemDefault,
        3,
        AlertLevels(0, 1, 2, 3),
      )
    )

    val statusChecker = system.actorOf(
      Props(new StatusChecker(checkEntities, ZonedDateTime.now().minusHours(2))))

    system.scheduler.scheduleWithFixedDelay(30 seconds, 30 seconds,
      statusChecker, Refresh(() => ZonedDateTime.now()))

    val route =
      concat(
        get {
          pathPrefix("maxlag") {
            val lagFuture = (
              statusChecker ? MaxLag
            ).mapTo[Int]
            onComplete(lagFuture) { lag =>
              complete(lag.toString)
            }
          }
        },
        get {
          path("missing") {
            val missingFuture = (
              statusChecker ? GetMissing
            ).mapTo[Seq[EntryStatus]]
            onComplete(missingFuture) { missing =>
              complete(missing.toString)
            }
          }
        },
        get {
          path("dashboard") {
            val allFuture = (
              statusChecker ? AllEntries
            ).mapTo[Seq[EntryReport]]
            onComplete(allFuture) {
              case Success(allRuns) =>
                val body =
                  allRuns
                    .map(_.entryRuns)
                    .flatten
                    .map( run =>
                      s"""|
                          |<tr bgcolor="${if(run.ok) "green" else "red"}">
                          |  <td>${run.entry.name}</td>
                          |  <td>${run.period}</td>
                          |</tr>
                          |""".stripMargin
                  ).mkString("\n")
                val dashboard =
                  s"""|<!DOCTYPE html>
                      |<html>
                      |  <head>
                      |     <title>Greenish Dashboard</title>
                      |     <meta http-equiv="refresh" content="30">
                      |  </head>
                      |  <body>
                      |     <table>
                      |        <tr>
                      |          <th>Job Name</th>
                      |          <th>Period</th>
                      |        </tr>
                      |        $body
                      |     </table>
                      |  </body>
                      |</html>
                      |""".stripMargin
                def httpEntity = HttpEntity(ContentTypes.`text/html(UTF-8)`, dashboard)
                complete(HttpResponse(entity = httpEntity))

              case Failure(error) =>
                complete(error.toString)
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
