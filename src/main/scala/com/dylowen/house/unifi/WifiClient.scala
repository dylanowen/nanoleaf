package com.dylowen.house
package unifi

import java.time.Instant

import io.circe.Decoder

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
sealed case class WifiClient(hostName: Option[String],
                             mac: String,
                             ip: Option[String],
                             uptime: FiniteDuration,
                             first_seen: Instant,
                             last_seen: Instant,
                             is_wired: Boolean) {
  override def toString: String = {
    val name: String = hostName
      .getOrElse(mac)
    s"$name($last_seen)"
  }
}

trait WifiClientJsonSupport {
  implicit val decodeFiniteDuration: Decoder[FiniteDuration] = Decoder.decodeLong
    .emap((secondsValue: Long) => {
      Right(secondsValue seconds)
    })

  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeLong
    .emap((secondsValue: Long) => {
      Right(Instant.ofEpochSecond(secondsValue))
    })
}