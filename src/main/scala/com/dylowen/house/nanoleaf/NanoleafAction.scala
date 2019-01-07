package com.dylowen.house
package nanoleaf

import java.time.Instant

import com.dylowen.house.unifi.WifiClient

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
sealed trait NanoleafAction {
  val time: Instant = Instant.now()
}

case object NoAction extends NanoleafAction {
  override def toString: String = getClass.getSimpleName
}

case object LightOn extends NanoleafAction

case object LightOff extends NanoleafAction

case class LightBrightness(brightness: Int) extends NanoleafAction

case class LightEffect(effect: String) extends NanoleafAction