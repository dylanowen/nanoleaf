package com.dylowen.unifi

import java.time.Instant

import com.dylowen.utils.UtilJsonSupport
import io.circe.Decoder
import spray.json.{JsObject, JsValue, RootJsonReader, deserializationError}

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
                             `first_seen`: Instant,
                             `last_seen`: Instant) {
  override def toString: String = {
    val name: String = hostName.getOrElse(getClass.getSimpleName)
    s"$name(${`last_seen`})"
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

/*
trait ClientJsonSupport extends UtilJsonSupport {
  implicit val deviceJsonFormat: RootJsonReader[WifiClient] = new RootJsonReader[WifiClient] {

    override def read(value: JsValue): WifiClient = value match {
      case obj: JsObject => {
        val hostname: Option[String] = fromField[Option[String]](obj, "hostname")
        val mac: String = fromField[String](obj, "mac")
        val ip: Option[String] = fromField[Option[String]](obj, "ip")
        val uptime: FiniteDuration = fromField[Long](obj, "uptime") seconds
        val firstSeen: Instant = Instant.ofEpochSecond(fromField[Long](obj, "first_seen"))
        val lastSeen: Instant = Instant.ofEpochSecond(fromField[Long](obj, "last_seen"))

        //WifiClient(hostname, mac, ip, uptime, firstSeen, lastSeen)
        WifiClient(hostname, mac, ip) //, uptime, firstSe, lastSeen)
      }
      case _ => deserializationError("device should be an object")
    }
  }
}
*/