package com.dylowen.unifi

import java.time.Instant

import com.dylowen.utils.UtilJsonSupport
import spray.json.{JsObject, JsValue, RootJsonReader, deserializationError}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
sealed case class Client(hostName: Option[String],
                         mac: String,
                         ip: Option[String],
                         uptime: FiniteDuration,
                         firstSeen: Instant,
                         lastSeen: Instant)

trait ClientJsonSupport extends UtilJsonSupport {
  implicit val deviceJsonFormat: RootJsonReader[Client] = new RootJsonReader[Client] {

    override def read(value: JsValue): Client = value match {
      case obj: JsObject => {
        val hostname: Option[String] = fromField[Option[String]](obj, "hostname")
        val mac: String = fromField[String](obj, "mac")
        val ip: Option[String] = fromField[Option[String]](obj, "ip")
        val uptime: FiniteDuration = fromField[Long](obj, "uptime") seconds
        val firstSeen: Instant = Instant.ofEpochSecond(fromField[Long](obj, "first_seen"))
        val lastSeen: Instant = Instant.ofEpochSecond(fromField[Long](obj, "last_seen"))

        Client(hostname, mac, ip, uptime, firstSeen, lastSeen)
      }
      case _ => deserializationError("device should be an object")
    }
  }
}