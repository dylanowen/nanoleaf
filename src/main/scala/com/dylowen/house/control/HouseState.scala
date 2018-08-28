package com.dylowen.house
package control

import com.dylowen.house.unifi.WifiClient

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
case class HouseState(clients: Seq[WifiClient])