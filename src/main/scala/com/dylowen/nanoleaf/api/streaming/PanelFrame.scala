package com.dylowen.nanoleaf.api.streaming

import com.dylowen.nanoleaf.api.StreamingData

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object PanelFrame {
  def apply(red: Int, green: Int, blue: Int, isStartFrame: Boolean = false): PanelFrame = {
    require(red >= 0 && red <= 255)
    require(green >= 0 && green <= 255)
    require(blue >= 0 && blue <= 255)

    new PanelFrame(red.toByte, green.toByte, blue.toByte, if (isStartFrame) -1 else 0)
  }
}

case class PanelFrame(red: Byte, green: Byte, blue: Byte, transitionTime: Byte) extends StreamingData {
  override val getBytes: Array[Byte] = Array(red, green, blue, 0x0.toByte, transitionTime)
}
