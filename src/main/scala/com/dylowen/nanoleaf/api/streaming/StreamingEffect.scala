package com.dylowen.nanoleaf.api.streaming

import com.dylowen.nanoleaf.api.StreamingData

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class StreamingEffect(panelEffects: PanelEffect*) extends StreamingData {
  override lazy val getBytes: Array[Byte] = Array(panelEffects.length.toByte) ++ panelEffects.map(_.getBytes)
    .fold(Array[Byte]())(_ ++ _)
}
