package com.dylowen.house
package unifi

import io.circe

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
private[unifi] final case class UnifiRPCJson(data: circe.Json, meta: circe.Json)