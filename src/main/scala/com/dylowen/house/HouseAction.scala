package com.dylowen.house

import java.time.Instant

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
sealed trait HouseAction {
  val time: Instant = Instant.now()
}

object NoAction extends HouseAction {
  override def toString: String = getClass.getSimpleName
}

case class LightOn() extends HouseAction

case class LightOff() extends HouseAction

case class LightBrightness(brightness: Int) extends HouseAction