package me.amanj.greenish

import io.circe.generic.extras.Configuration

package object models {
  private[models] implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
}
