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
sealed case class Device(name: Option[String],
                        mac: String,
                     uptime: FiniteDuration,
                     firstSeen: Instant,
                     lastSeen: Instant)

trait DeviceJsonSupport extends UtilJsonSupport {
  implicit val deviceJsonFormat: RootJsonReader[Device] = new RootJsonReader[Device] {

    override def read(value: JsValue): Device = value match {
      case obj: JsObject => {
        val name: Option[String] = fromField[Option[String]](obj, "name")
        val mac: String = fromField[String](obj, "mac")
        val uptime: FiniteDuration = fromField[Long](obj, "uptime") seconds
        val firstSeen: Instant = Instant.ofEpochSecond(fromField[Long](obj, "first_seen"))
        val lastSeen: Instant = Instant.ofEpochSecond(fromField[Long](obj, "last_seen"))

        Device(name, mac, uptime, firstSeen, lastSeen)
      }
      case _ => deserializationError("device should be an object")
    }
  }
}