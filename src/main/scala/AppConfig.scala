package me.amanj.greenish

import com.typesafe.config.{Config, ConfigFactory}
import java.time.ZoneId
import scala.util.Try
import models._
import scala.jdk.CollectionConverters._

case class AppConfig(groups: Seq[Group], namespace: Option[String], refreshInSeconds: Int,
  address: String, port: Int,
  )
object AppConfig {
  def apply(): AppConfig = {
    val config = ConfigFactory.load()
    val appConfig = config.getConfig("check-groups")
    val refreshRate = appConfig.getInt("refresh-in-seconds")
    val port = appConfig.getInt("port")
    val namespace = Try(appConfig.getString("namespace")).toOption
    val address = appConfig.getString("binding-address")
    new AppConfig(readEntries(appConfig), namespace, refreshRate, address, port)
  }

  private[this] def readEntries(config: Config): Seq[Group] = {
    val defaultOwner = config.getOptionStringWithDefault("default-owner", None)
    val defaultPeriodCheckOffset = config.getInt("default-period-check-offset")
    val defaultTimePattern = config.getString("default-period-pattern")
    val defaultFrequency = config.getString("default-job-run-frequency")
    val defaultTimezone = config.getString("default-timezone")
    val defaultLookback = config.getInt("default-lookback")
    val defaultGreatAt = config.getInt("default-great-at")
    val defaultNormalAt = config.getInt("default-normal-at")
    val defaultWarnAt = config.getInt("default-warn-at")
    val defaultErrorAt = config.getInt("default-error-at")
    val defaultStartAt = config.getLong("default-start-at")
    val globalEnv = config.getEnv("env", Seq.empty)

    config.getConfigList("groups").asScala.zipWithIndex.map { case (groupConfig, index) =>
      val groupName = groupConfig.getString("group-name")
      val groupOwner = groupConfig.getOptionStringWithDefault("group-owner", defaultOwner)
      val groupPeriodCheckOffset =
        groupConfig.getIntWithDefault("group-period-check-offset", defaultPeriodCheckOffset)
      val groupTimePattern = groupConfig.getStringWithDefault(
        "group-period-pattern", defaultTimePattern)
      val groupFrequency = groupConfig.getStringWithDefault(
        "group-job-run-frequency", defaultFrequency)
      val groupTimezone = groupConfig.getStringWithDefault(
        "group-timezone", defaultTimezone)
      val groupLookback = groupConfig.getIntWithDefault(
        "group-lookback", defaultLookback)
      val groupGreatAt = groupConfig.getIntWithDefault(
        "group-great-at", defaultGreatAt)
      val groupNormalAt = groupConfig.getIntWithDefault(
        "group-normal-at", defaultNormalAt)
      val groupWarnAt = groupConfig.getIntWithDefault(
        "group-warn-at", defaultWarnAt)
      val groupErrorAt = groupConfig.getIntWithDefault(
        "group-error-at", defaultErrorAt)
      val groupStartAt = groupConfig.getLongWithDefault(
        "group-start-at", defaultStartAt)
      val groupEnv = groupConfig.getEnv("env", globalEnv)

      val checkEntries = groupConfig.getConfigList("job-entries")
        .asScala.zipWithIndex.map { case (jobConfig, index) =>
          val jobName = jobConfig.getString("job-name")
          val jobOwner = jobConfig.getOptionStringWithDefault("job-owner", groupOwner)
          val prometheusId = normalizePrometheusId(
            jobConfig.getStringWithDefault(
              "prometheus-id", s"$groupName $jobName"))
          val cmd = jobConfig.getString("check-command")
          val jobPeriodCheckOffset = jobConfig.getIntWithDefault(
            "job-period-check-offset", groupPeriodCheckOffset)
          val timePattern = jobConfig.getStringWithDefault(
            "period-pattern", groupTimePattern)
          val frequency = toFrequency(
            jobConfig.getStringWithDefault(
              "job-run-frequency", groupFrequency))
          val timezone = ZoneId.of(
            jobConfig.getStringWithDefault("timezone", groupTimezone))
          val lookback = jobConfig.getIntWithDefault(
            "lookback", groupLookback)
          val greatAt = jobConfig.getIntWithDefault(
            "great-at", groupGreatAt)
          val normalAt = jobConfig.getIntWithDefault(
            "normal-at", groupNormalAt)
          val warnAt = jobConfig.getIntWithDefault(
            "warn-at", groupWarnAt)
          val errorAt = jobConfig.getIntWithDefault(
            "error-at", groupErrorAt)
          val startAt = jobConfig.getLongWithDefault(
            "start-at", groupStartAt)
          val jobEnv = jobConfig.getEnv("env", groupEnv)

          Job(
            index,
            jobName,
            jobOwner,
            prometheusId,
            cmd,
            timePattern,
            frequency,
            jobPeriodCheckOffset,
            timezone,
            lookback,
            startAt,
            AlertLevels(greatAt, normalAt, warnAt, errorAt),
            jobEnv,
          )
        }.toSeq
      Group(index, groupName, checkEntries)
    }.toSeq
  }

  private[greenish] def normalizePrometheusId(id: String): String = {
    val spacelessId = id.replaceAll("(\\s|-)+","_").toLowerCase
    val pattern = "[a-zA-Z_][a-zA-Z0-9_]*"
    if(!spacelessId.matches(pattern)) {
      throw new Exception(
        s"""|$id: Invalid prometheus label ID, please provide a valid one.
            |Prometheus label names should match: "$pattern"""".stripMargin)
    }
    spacelessId
  }

  private[greenish] def toFrequency(freq: String): CheckFrequency = {
    freq.toLowerCase match {
      case "hourly" => Hourly
      case "daily" => Daily
      case "monthly" => Monthly
      case "annually" => Annually
      case _         =>
        try {
          Cron(freq)
        } catch {
          case e: IllegalArgumentException =>
            throw new Exception(
              s"""|${e.getMessage}
                  |$freq: unsupported frequency, supported frequenices are:
                  |hourly, daily, monthly, annually and Unix cron syntax"""
                   .stripMargin)
        }
    }
  }

  implicit class ConfigExt[C <: Config](self: Config) {
    def getStringWithDefault(path: String, default: String): String =
      if(self.hasPath(path))
        self.getString(path)
      else default

    def getIntWithDefault(path: String, default: Int): Int =
      if(self.hasPath(path))
        self.getInt(path)
      else default

    def getLongWithDefault(path: String, default: Long): Long =
      if(self.hasPath(path))
        self.getLong(path)
      else default

    def getOptionStringWithDefault(path: String,
        default: Option[String]): Option[String] =
      if(self.hasPath(path))
        Some(self.getString(path))
      else default


    def getEnv(path: String, parent: Seq[(String, String)]): Seq[(String, String)] =
      if(self.hasPath(path)) {
        val localEnv = self.getConfig("env")
          .entrySet.asScala
          .map(e => (e.getKey, e.getValue.unwrapped.asInstanceOf[String]))
          .toMap

        val overriddenParent = parent.filterNot { case (k, _) =>
          localEnv.contains(k)
        }

        (localEnv.toSeq ++ overriddenParent).sorted
      } else parent
  }
}
