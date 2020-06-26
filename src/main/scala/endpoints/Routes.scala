package me.amanj.greenish.endpoints

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import scala.concurrent.duration.Duration
import io.circe.syntax._
import io.circe.Printer
import me.amanj.greenish.models._
import me.amanj.greenish.checker._

class Routes(statusChecker: ActorRef) {
  private[this] implicit val timeout = Timeout(Duration.fromNanos(5000000L))
  private[this] val jsonPrinter = Printer (
    dropNullValues = true,
    indent=""
  )

  private[this] val maxlag = get {
    pathPrefix("maxlag") {
      val lagFuture = (
        statusChecker ? MaxLag
      ).mapTo[Int]
      onComplete(lagFuture) { lag =>
        complete(lag.map(o => jsonPrinter.print(o.asJson)))
      }
    }
  }

  private[this] val summary = get {
    pathPrefix("summary") {
      val lagFuture = (
        statusChecker ? Summary
      ).mapTo[Seq[GroupStatusSummary]]
      onComplete(lagFuture) { lag =>
        complete(lag.map(o => jsonPrinter.print(o.asJson)))
      }
    }
  }

  private[this] val missing = get {
    path("missing") {
      val missingFuture = (
        statusChecker ? GetMissing
      ).mapTo[Seq[GroupStatus]]
      onComplete(missingFuture) { missing =>
        complete(missing.map(o => jsonPrinter.print(o.asJson)))
      }
    }
  }

  private[this] val dashboard = get {
    path("dashboard") {
      val allFuture = (
        statusChecker ? AllEntries
      ).mapTo[Seq[GroupStatus]]
      onComplete(allFuture) { completed =>
        complete(completed.map(o => jsonPrinter.print(o.asJson)))
      }
    }
  }

  val routes = concat (
    maxlag, summary, missing, dashboard
  )
}