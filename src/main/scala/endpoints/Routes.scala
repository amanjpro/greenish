package me.amanj.greenish.endpoints

import java.time.ZonedDateTime
import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.server.Directives._
import scala.concurrent.duration.Duration
import io.circe.syntax._
import io.circe.Printer
import me.amanj.greenish.models._
import me.amanj.greenish.stats._
import me.amanj.greenish.checker._
import akka.http.scaladsl.model.HttpResponse
import scala.util.Success

class Routes(namespace: Option[String],
    statusChecker: ActorRef,
    statsActor: ActorRef,
    goodRefreshRecency: Long,
    now: () => ZonedDateTime = () => ZonedDateTime.now) {
  private[this] implicit val timeout = Timeout(Duration.fromNanos(5000000L))
  private[this] val jsonPrinter = Printer (
    dropNullValues = true,
    indent=""
  )

  private[this] val maxlag = get {
    path("maxlag") {
      val lagFuture = (
        statusChecker ? MaxLag
      ).mapTo[Lag]
      onComplete(lagFuture) { lag =>
        complete(lag.map(o => jsonPrinter.print(o.asJson)))
      }
    }
  }

  private[this] val summary = get {
    path("summary") {
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

  private[this] val refreshState = get {
    path("state" / "refresh") {
      statusChecker ! Refresh(now)
      complete(jsonPrinter.print(okJson("State refresh is scheduled")))
    }
  }

  private[this] val refreshGroup = get {
    path("group" / IntNumber / "refresh") { id =>
      val statusFuture = (
        statusChecker ? RefreshGroup(now, id)
      ).mapTo[Boolean]

      onComplete(statusFuture) {
        case Success(true) =>
          complete(jsonPrinter.print(okJson("Group status refresh is scheduled")))
        case _                  =>
          val error = jsonPrinter.print(errorJson("Group id does not exist"))
          complete(HttpResponse(StatusCodes.BadRequest, entity = error))
      }
    }
  }

  private[this] val refreshJob = get {
    path("group" / IntNumber / "job" / IntNumber / "refresh") {
      (gid, jid) =>
        val statusFuture = (
          statusChecker ? RefreshJob(now, gid, jid)
        ).mapTo[Boolean]
        onComplete(statusFuture) {
          case Success(true) =>
            complete(jsonPrinter.print(okJson("Job status refresh is scheduled")))
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
      val json = jsonPrinter.print(sysinfo(namespace))
      complete(json)
    }
  }

  private[this] val prometheus = get {
    path("prometheus") {
      val statsFuture =
        (statsActor ? GetPrometheus)
          .mapTo[StatsCollector.MetricsEntity]
      onComplete(statsFuture) { entity =>
        complete(entity)
      }
    }
  }

  private[this] val health = get {
    path("health") {
      val entriesFuture = (statusChecker ? AllEntries)
        .mapTo[Seq[GroupStatus]]

      onComplete(entriesFuture) { entity =>
        val health = entity.map( groups =>
            Routes.isHealthy(groups, goodRefreshRecency)).getOrElse(false)
        val json = jsonPrinter.print(healthJson(health))
        complete(json)
      }
    }
  }

  val routes =
    getJob ~ getGroup ~ refreshState ~ refreshGroup ~ refreshJob ~
      maxlag ~ summary ~ missing ~ state ~ dashboard ~ system ~
      prometheus ~ health
}

object Routes {
  private[endpoints] def isHealthy(groups: Seq[GroupStatus],
      recency: Long): Boolean = {
    val now = System.currentTimeMillis
    groups.map { group =>
      group.status.filterNot { job =>
        (now - job.updatedAt) > recency || job.periodHealth.isEmpty
      }.length
    }.exists(_ > 0)
  }
}
