package com.dylowen.house.control

import com.dylowen.house.Seq
import com.dylowen.house.unifi.WifiClient

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Sep-2018
  */
object ActiveClients {

  lazy val Empty: ActiveClients = ActiveClients(Seq(), Seq())

  def apply(): ActiveClients = Empty
}

case class ActiveClients(phones: Seq[WifiClient], wirelessClients: Seq[WifiClient])
