package com.dylowen.mdns.record

import java.net.InetAddress

import com.dylowen.mdns._
import com.dylowen.utils.Binary.{ShortBits, ShortMask}

import scala.collection.{immutable, mutable}
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object ResourceRecord {

  def apply(bytes: Array[Byte], offset: Offset, nameCache: NameCache, numberOfAnswers: Int): Seq[ResourceRecord] = {
    val builder: mutable.Builder[ResourceRecord, Seq[ResourceRecord]] = Seq.newBuilder

    for (_ <- 0 until numberOfAnswers) {
      builder += apply(bytes, offset, nameCache)
    }

    builder.result()
  }

  def apply(bytes: Array[Byte], offset: Offset, nameCache: NameCache): ResourceRecord = {
    val names: Seq[String] = Message.readNames(bytes, offset, nameCache)

    val recordType: RecordType = RecordType(offset.slice(bytes, 2))

    val responseType: Int = bytesToShort(bytes, offset)
    val flushCache: Boolean = (responseType >> (ShortBits - 1)) == 0x01
    val responseClass: Int = responseType & (ShortMask >> 1)
    val timeToLive: FiniteDuration = bytesToInt(bytes, offset) seconds

    val dataLength: Int = bytesToShort(bytes, offset)
    val innerOffset: Offset = new Offset(offset.i)

    // eat up our data length
    offset.i += dataLength

    recordType match {
      case RecordType.A => {
        val addressBytes: Array[Byte] = innerOffset.slice(bytes, 4)
        val address: InetAddress = InetAddress.getByAddress(addressBytes)

        AResource(names, flushCache, responseClass, timeToLive, address)
      }
      case RecordType.PTR => {
        val domain: Seq[String] = Message.readNames(bytes, innerOffset, nameCache)

        PtrResource(names, flushCache, responseClass, timeToLive, domain)
      }
      case RecordType.TXT => {
        TxtResource(bytes, innerOffset, dataLength, names, flushCache, responseClass, timeToLive)
      }
      case RecordType.AAAA => {
        ???
      }
      case recordType: RecordType => {
        UnknownResource(names, recordType, flushCache, responseClass, timeToLive)
      }
    }
  }
}

trait ResourceRecord {
  val names: Seq[String]
  val recordType: RecordType
  val flushCache: Boolean
  val responseClass: Int
  val timeToLive: FiniteDuration
}


case class UnknownResource(override val names: Seq[String],
                           override val recordType: RecordType,
                           override val flushCache: Boolean,
                           override val responseClass: Int,
                           override val timeToLive: FiniteDuration) extends ResourceRecord