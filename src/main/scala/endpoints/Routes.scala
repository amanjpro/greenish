package me.amanj.greenish.endpoints

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import scala.concurrent.duration.Duration
import io.circe.syntax._
import io.circe.Printer
import me.amanj.greenish.models
import models.{sysinfo => _, _}
import me.amanj.greenish.checker._
import akka.http.scaladsl.model.HttpResponse
import scala.util.Success

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
      ).mapTo[Lag]
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

  private[this] val state = get {
    path("state") {
      val allFuture = (
        statusChecker ? AllEntries
      ).mapTo[Seq[GroupStatus]]
      onComplete(allFuture) { completed =>
        complete(completed.map(o => jsonPrinter.print(o.asJson)))
      }
    }
  }

  private[this] val getGroup = get {
    path("group" / IntNumber) { id =>
      val groupFuture = (
        statusChecker ? GetGroupStatus(id)
      ).mapTo[Option[GroupStatus]]

      onComplete(groupFuture) {
        case Success(Some(group)) =>
          complete(jsonPrinter.print(group.asJson))
        case _                  =>
          val error = jsonPrinter.print(errorJson("Group id does not exist"))
          complete(HttpResponse(StatusCodes.BadRequest, entity = error))
      }
    }
  }

  private[this] val getJob = get {
    path("group" / IntNumber / "job" / IntNumber) {
      (gid, jid) =>
        val jobFuture = (
          statusChecker ? GetJobStatus(gid, jid)
        ).mapTo[Option[JobStatus]]
        onComplete(jobFuture) {
          case Success(Some(job)) =>
            complete(jsonPrinter.print(job.asJson))
          case _                  =>
            val error = jsonPrinter
              .print(errorJson("Group id and/or job id does not exist"))
            complete(HttpResponse(StatusCodes.BadRequest, entity = error))
        }
    }
  }

  private[this] val dashboard =
      (get & pathPrefix("dashboard")) {
        (pathEndOrSingleSlash &
          redirectToTrailingSlashIfMissing(StatusCodes.TemporaryRedirect)) {
            getFromResource("dashboard/index.html")
        } ~ {
          getFromResourceDirectory("dashboard")
        }
      }

  private[this] val system = get {
    path("system") {
      val json = jsonPrinter.print(models.sysinfo())
      complete(json)
    }
  }

  val routes = getJob ~ getGroup ~ maxlag ~ summary ~ missing ~ state ~ dashboard ~ system
}
