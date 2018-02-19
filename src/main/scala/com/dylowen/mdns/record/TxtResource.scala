package com.dylowen.mdns.record

import com.dylowen.mdns.Offset

import scala.annotation.tailrec
import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object TxtResource {
  def apply(bytes: Array[Byte],
            offset: Offset,
            dataLength: Int,
            names: Seq[String],
            flushCache: Boolean,
            responseClass: Int,
            timeToLive: FiniteDuration): TxtResource = {
    val text: immutable.Map[String, String] = readText(bytes, offset, dataLength)

    new TxtResource(names, flushCache, responseClass, timeToLive, text)
  }

  @tailrec
  def readText(bytes: Array[Byte],
               offset: Offset,
               remainingBytes: Int,
               text: List[(String, String)] = List()): immutable.Map[String, String] = {
    if (remainingBytes > 0) {
      val length: Byte = offset.next(bytes)
      val keyValue: String = new String(offset.slice(bytes, length))
      val split: Int = keyValue.indexOf('=')

      readText(bytes, offset, remainingBytes - length - 1, (keyValue.substring(0, split), keyValue.substring(split + 1)) +: text)
    }
    else {
      text.reverse.toMap
    }
  }
}

case class TxtResource(override val names: Seq[String],
                       override val flushCache: Boolean,
                       override val responseClass: Int,
                       override val timeToLive: FiniteDuration,
                       text: immutable.Map[String, String]) extends ResourceRecord {
  override val recordType: RecordType = RecordType.TXT
}