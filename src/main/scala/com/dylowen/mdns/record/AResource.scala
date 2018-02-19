package com.dylowen.mdns.record

import java.net.{Inet4Address, InetAddress}

import scala.concurrent.duration.FiniteDuration

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class AResource(override val names: Seq[String],
                     override val flushCache: Boolean,
                     override val responseClass: Int,
                     override val timeToLive: FiniteDuration,
                     address: InetAddress) extends ResourceRecord {
  override val recordType: RecordType = RecordType.A
}