package com.dylowen.nanoleaf.api

import com.dylowen.utils.UtilJsonSupport
import spray.json.RootJsonFormat

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

trait NanoLeafBrightnessJsonSupport extends UtilJsonSupport {
  implicit val NanoLeafBrightnessJsonFormat: RootJsonFormat[NanoLeafBrightness] = jsonFormat3(NanoLeafBrightness.apply)
}
