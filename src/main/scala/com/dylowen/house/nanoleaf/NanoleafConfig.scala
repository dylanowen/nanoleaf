package com.dylowen.house
package nanoleaf

import com.dylowen.house.utils._
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
    val manualModeEffect: Option[String] = config.optional(_.getString)("manual-mode")

    NanoleafConfig(auth, minBrightness, dimHour, manualModeEffect)
  }
}

case class NanoleafConfig(auth: String,
                          minBrightness: Int,
                          dimHour: Int,
                          manualMode: Option[String])