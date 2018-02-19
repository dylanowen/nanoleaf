package com.dylowen.unifi

import java.net.URL

import com.dylowen.utils._
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
    } yield UnifiConfigImpl(baseUrl, username, password)
  }
}

trait UnifiConfig {
  val baseUrl: URL
  val username: String
  val password: String
}

case class UnifiConfigImpl(override val baseUrl: URL,
                           override val username: String,
                           override val password: String) extends UnifiConfig