package com.dylowen.house.nanoleaf.mdns

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
case class NanoleafAddress(id: String, name: String, url: String) {
  override def toString: String = name
}