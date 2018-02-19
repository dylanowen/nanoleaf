package com.dylowen.mdns.record

import scala.concurrent.duration.FiniteDuration

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class PtrResource(override val names: Seq[String],
                       override val flushCache: Boolean,
                       override val responseClass: Int,
                       override val timeToLive: FiniteDuration,
                       domain: Seq[String]) extends ResourceRecord {
  override val recordType: RecordType = RecordType.PTR
}
