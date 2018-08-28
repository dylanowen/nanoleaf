package com.dylowen.house
package nanoleaf.api

import scala.language.implicitConversions

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object NanoLeafBrightness {
  implicit def brightnessToInt(brightness: NanoLeafBrightness): Int = brightness.value
}

final case class NanoLeafBrightness(value: Int, min: Int, max: Int)