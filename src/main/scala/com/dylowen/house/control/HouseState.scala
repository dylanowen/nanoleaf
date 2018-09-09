package com.dylowen.house
package control

import com.dylowen.house.unifi.WifiClient

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
case class HouseState(phones: Seq[WifiClient],
                      wirelessClients: Seq[WifiClient],
                      lastPhones: Seq[WifiClient],
                      lastWirelessClients: Seq[WifiClient]) {
  override def toString: String = {
    s"${getClass.getSimpleName}(" +
      s"phones: ${phones.length} Δ${newPhones.length} " +
      s"wirelessClients: ${wirelessClients.length} Δ${newWirelessClients.length}" +
    s")"
  }

  lazy val newPhones: Seq[WifiClient] = newClients(phones, lastPhones)

  lazy val newWirelessClients: Seq[WifiClient] = newClients(wirelessClients, lastWirelessClients)

  private def newClients(clients: Seq[WifiClient], lastClients: Seq[WifiClient]): Seq[WifiClient] = {
    clients.filterNot((client: WifiClient) => {
      // check if the last phone list contains our phone
      lastClients.exists(_.mac == client.mac)
    })
  }
}