package com.dylowen.nanoleaf.api.streaming

import com.dylowen.nanoleaf.api.{PanelId, StreamingData}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class PanelEffect(id: PanelId, streamingFrames: PanelFrame*) extends StreamingData {
  override lazy val getBytes: Array[Byte] = {
    Array(id.id.toByte) ++
      Array(streamingFrames.length.toByte) ++
      streamingFrames.map(_.getBytes).fold(Array[Byte]())(_ ++ _)
  }
}
