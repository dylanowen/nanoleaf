package com.dylowen.nanoleaf.api

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class PanelId(id: Int) extends StreamingData {
  override val getBytes: Array[Byte] = Array(id.toByte)
}
