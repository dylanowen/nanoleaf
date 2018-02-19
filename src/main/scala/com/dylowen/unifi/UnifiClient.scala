package com.dylowen.unifi

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object UnifiClient {
  def apply(): UnifiClient = {
    new UnifiClient(null)
  }
}

class UnifiClient(cookies: String) {

}
