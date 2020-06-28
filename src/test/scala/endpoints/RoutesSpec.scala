package me.amanj.greenish.endpoints

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.concurrent.Eventually
import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.duration._
import scala.language.postfixOps
import me.amanj.greenish.models._
import me.amanj.greenish.checker._
import io.circe.parser._
import java.io.File

class RoutesSpec()
    extends AnyWordSpecLike
    with Matchers
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with Eventually{

  val dir1 = new File("/tmp/2020-06-25-14")
  implicit val patience: PatienceConfig = PatienceConfig(1 minute, 1 second)
  override def beforeAll: Unit = {
    dir1.mkdirs
    checker = system.actorOf(Props(new StatusChecker(Seq(group1), Seq.empty)))
    checker ! Refresh(() => ZonedDateTime.parse("2020-06-25T15:05:30+01:00[UTC]"))
    routes = new Routes(checker)
  }
  override def afterAll: Unit = {
    dir1.delete
    cleanUp()
  }

  val lsScript = getClass.getResource("/test-ls").getFile
  val lsEnvScript = getClass.getResource("/test-ls-env").getFile

  val job1 = Job(1, "job1", s"$lsScript /tmp",
    "yyyy-MM-dd-HH", Hourly, ZoneId.of("UTC"),
    2, AlertLevels(0, 1, 2, 3),
  )

  val group1 = Group(1, "group1", Seq(job1))

  var checker: ActorRef = _
  var routes: Routes = _

  "Routes" must {
    "properly handle GET/maxlag request" in {
      eventually {
        Get("/maxlag") ~> routes.routes ~> check {
          val actual = parse(responseAs[String])
            .flatMap(_.as[Lag]).getOrElse(null)
          val expected = Lag(1)
          actual shouldBe expected
        }
      }
    }

    "properly handle GET/state request" in {
      eventually {
        Get("/state") ~> routes.routes ~> check {
          val actual = parse(responseAs[String])
            .flatMap(_.as[Seq[GroupStatus]]).getOrElse(null)
          val expected = Seq(GroupStatus(group1, Seq(JobStatus(
              job1, Seq(
                PeriodHealth("2020-06-25-14", true),
                PeriodHealth("2020-06-25-15", false),
                )
            ))))
          actual shouldBe expected
        }
      }
    }

    "properly handle GET/missing request" in {
      eventually {
        Get("/missing") ~> routes.routes ~> check {
          val actual = parse(responseAs[String])
            .flatMap(_.as[Seq[GroupStatus]]).getOrElse(null)
          val expected = Seq(GroupStatus(group1, Seq(JobStatus(
              job1, Seq(
                PeriodHealth("2020-06-25-15", false),
                )
            ))))
          actual shouldBe expected
        }
      }
    }

    "properly handle GET/summary request" in {
      eventually {
        Get("/summary") ~> routes.routes ~> check {
          val actual = parse(responseAs[String])
            .flatMap(_.as[Seq[GroupStatusSummary]]).getOrElse(null)
          val expected = Seq(GroupStatusSummary(group1.name, Seq(JobStatusSummary(
              job1.name, 1, Normal
            ))))
          actual shouldBe expected
        }
      }
    }
  }

}
