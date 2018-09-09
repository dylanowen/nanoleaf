package com.dylowen.house.control

import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Sep-2018
  */
object ClientConfig {
  def apply(config: Config): ClientConfig = {
    val effect: String = config.getString("effect")

    ClientConfig(effect)
  }

  def configs(config: Config): Map[String, ClientConfig] = {
    config.root().entrySet().asScala
      .map(_.getKey)
      .map((key: String) => {
        val rawClientConfig: Config = config.getConfig(s""""$key"""")

        key -> ClientConfig(
          effect = rawClientConfig.getString("effect")
        )
      })
      .toMap
  }
}

case class ClientConfig(effect: String)
