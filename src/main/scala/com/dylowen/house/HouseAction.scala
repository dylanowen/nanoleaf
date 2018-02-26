package com.dylowen.house

import java.time.Instant

import com.dylowen.nanoleaf.api.NanoLeafClient

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
sealed trait HouseAction

case object NoAction extends HouseAction

case class LightOn(time: Instant) extends HouseAction

case class LightOff(time: Instant) extends HouseAction

case class LightBrightness(brightness: Int, time: Instant, client: NanoLeafClient) extends HouseAction