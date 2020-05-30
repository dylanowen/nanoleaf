package com.dylowen.house.control

import com.dylowen.house.Seq
import com.dylowen.house.unifi.NetworkClient

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Sep-2018
  */
object WifiClients {

  lazy val Empty: WifiClients = WifiClients(Seq(), Seq())

  def apply(): WifiClients = Empty
}

case class WifiClients(phones: Seq[NetworkClient], wirelessClients: Seq[NetworkClient])
