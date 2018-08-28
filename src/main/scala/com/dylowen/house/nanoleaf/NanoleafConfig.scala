package com.dylowen.house
package nanoleaf

import java.net.URL

import com.dylowen.house.utils._
import com.dylowen.house.unifi.UnifiConfig
import com.typesafe.config.Config

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object NanoleafConfig {
  def apply(config: Config): NanoleafConfig = {
    val auth: String = config.getString("auth")
    val minBrightness: Int = config.optional(_.getInt)("min-brightness")
      .getOrElse(22)
    val dimHour: Int = config.optional(_.getInt)("dim-hour")
      .getOrElse(5)

    NanoleafConfig(auth, minBrightness, dimHour)
  }
}

case class NanoleafConfig(auth: String, minBrightness: Int, dimHour: Int)