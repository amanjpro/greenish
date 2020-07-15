package me.amanj.greenish

import com.typesafe.config.{Config, ConfigFactory}
import java.time.ZoneId
import models._
import scala.jdk.CollectionConverters._

case class AppConfig(groups: Seq[Group], refreshInSeconds: Int,
  address: String, port: Int,
  env: Seq[(String, String)])
object AppConfig {
  def apply(): AppConfig = {
    val config = ConfigFactory.load()
    val appConfig = config.getConfig("check-groups")
    val refreshRate = appConfig.getInt("refresh-in-seconds")
    val port = appConfig.getInt("port")
    val address = appConfig.getString("binding-address")
    val env = appConfig.getConfig("env")
      .entrySet.asScala
      .map(e => (e.getKey, e.getValue.unwrapped.asInstanceOf[String]))
      .toSeq
    new AppConfig(readEntries(appConfig), refreshRate, address, port, env)
  }

  private[this] def readEntries(config: Config): Seq[Group] = {
    val defaultPeriodCheckOffset = config.getInt("default-period-check-offset")
    config.getConfigList("groups").asScala.zipWithIndex.map { case (groupConfig, index) =>
      val name = groupConfig.getString("group-name")
      val groupPeriodCheckOffset =
        groupConfig.getIntWithDefault("group-period-check-offset", defaultPeriodCheckOffset)
      val checkEntries = groupConfig.getConfigList("job-entries")
        .asScala.zipWithIndex.map { case (jobConfig, index) =>
          val name = jobConfig.getString("job-name")
          val prometheusId = s"job-$index"
          val cmd = jobConfig.getString("check-command")
          val jobPeriodCheckOffset =
            jobConfig.getIntWithDefault("job-period-check-offset", groupPeriodCheckOffset)
          val timePattern = jobConfig.getString("period-pattern")
          val timezone = ZoneId.of(jobConfig.getString("timezone"))
          val lookback = jobConfig.getInt("lookback")
          val greatAt = jobConfig.getInt("great-at")
          val normalAt = jobConfig.getInt("normal-at")
          val warnAt = jobConfig.getInt("warn-at")
          val errorAt = jobConfig.getInt("error-at")

          val frequency = jobConfig.getString("job-run-frequency").toLowerCase match {
            case "hourly" => Hourly
            case "daily" => Daily
            case "monthly" => Monthly
            case "annually" => Annually
            case _         =>
              throw new Exception(
                """|Unsupported frequency, supported frequenices are:
                   |hourly, daily, monthly and annually""".stripMargin)
          }
          Job(
            index,
            name,
            prometheusId,
            cmd,
            timePattern,
            frequency,
            jobPeriodCheckOffset,
            timezone,
            lookback,
            AlertLevels(greatAt, normalAt, warnAt, errorAt))
        }.toSeq
      Group(index, name, checkEntries)
    }.toSeq
  }

  implicit class ConfigExt[C <: Config](self: Config) {
    def getIntWithDefault(path: String, default: Int): Int =
      if(self.hasPath(path))
        self.getInt(path)
      else default
  }
}
