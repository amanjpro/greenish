package me.amanj.greenish

import me.amanj.greenish.models.PeriodHealth
import java.io.File

package object checker {
  protected[checker] def computeOldest(periodHealths: Seq[PeriodHealth]): Int = {
    val missingIndex = periodHealths.indexWhere(!_.ok)
    if(missingIndex == -1) 0
    else periodHealths.length - missingIndex
  }

  private[this] implicit class FileOps(p: String) {
    def /(c: String): String = s"$p${File.separator}$c"
  }

  private[checker] val outputDir =
    new File("" / "tmp" / "greenish" / "stdout")
  outputDir.mkdirs

  def debugFile(groupId: Int, jobId: Int): String = {
    val fileName =
      outputDir.toString / s"group-$groupId-job-$jobId-stdout.txt"
    fileName
  }
}
