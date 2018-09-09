package com.dylowen.house
package nanoleaf.api

import scala.language.implicitConversions

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object Brightness {
  implicit def brightnessToInt(brightness: Brightness): Int = brightness.value
}

final case class Brightness(value: Int, min: Int, max: Int)