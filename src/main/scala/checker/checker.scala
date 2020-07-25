package me.amanj.greenish

import me.amanj.greenish.models.PeriodHealth

package object checker {
  protected[checker] def computeOldest(periodHealths: Seq[PeriodHealth]): Int = {
    val missingIndex = periodHealths.indexWhere(!_.ok)
    if(missingIndex == -1) 0
    else periodHealths.length - missingIndex
  }
}
