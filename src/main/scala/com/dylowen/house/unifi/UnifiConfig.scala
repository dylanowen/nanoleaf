package com.dylowen.house
package unifi

import java.net.URL

import com.dylowen.house.utils._
import com.typesafe.config.Config


/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object UnifiConfig {
  def apply(config: Config): Option[UnifiConfig] = {
    for {
      baseUrl <- config.optional(_.getUrl)("unifi.base-url")
      username <- config.optionalString("unifi.username")
      password <- config.optionalString("unifi.password")
    } yield UnifiConfig(baseUrl, username, password)
  }
}

case class UnifiConfig(baseUrl: URL,
                       username: String,
                       password: String)