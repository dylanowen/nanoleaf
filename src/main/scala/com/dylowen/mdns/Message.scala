package com.dylowen.mdns

import com.dylowen.mdns.record.ResourceRecord
import com.dylowen.utils.Binary._

import scala.annotation.tailrec
import scala.collection.mutable

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object Message {

  private val EndSignalByte: Byte = 0x00
  private val CompressedSignalByte: Byte = 0xC0

  def apply(bytes: Array[Byte]): Message = {
    val offset: Offset = new Offset()

    val nameCache: NameCache = mutable.Map()

    val transactionId: Int = bytesToShort(bytes, offset)
    //flags
    offset.i += 2

    val numberOfQuestions: Int = bytesToShort(bytes, offset)
    val numberOfAnswers: Int = bytesToShort(bytes, offset)
    val numberOfAuthorityRecords: Int = bytesToShort(bytes, offset)
    val numberOfAdditionalRecords: Int = bytesToShort(bytes, offset)

    val queries: Seq[Query] = Query(bytes, offset, nameCache, numberOfQuestions)

    val answers: Seq[ResourceRecord] = ResourceRecord(bytes, offset, nameCache, numberOfAnswers)

    val authorityRecords: Seq[ResourceRecord] = ResourceRecord(bytes, offset, nameCache, numberOfAuthorityRecords)

    val additionalRecords: Seq[ResourceRecord] = ResourceRecord(bytes, offset, nameCache, numberOfAdditionalRecords)

    //val answers: Seq[]

    null
  }

  @tailrec
  def readNames(bytes: Array[Byte], offset: Offset, nameCache: NameCache, names: List[String] = List()): Seq[String] = {
    val cacheIndex: Byte = offset.i
    val length: Byte = offset.next(bytes)

    length match {
      case EndSignalByte => names.reverse
      case CompressedSignalByte => {
        names.reverse ++ readFromCache(bytes, offset, nameCache)
      }
      case _ => {
        // read a name out based on the length
        val nameBytes: Array[Byte] = bytes.slice(offset.i, offset.i + length)
        val name: String = new String(nameBytes)
        offset.i += length

        val nextByte: Byte = offset.peek(bytes)
        val next: Option[Byte] = if (nextByte != EndSignalByte) {
          if (nextByte != CompressedSignalByte) {
            // grab the index of the next segment
            Some(offset.i)
          }
          else {
            // since the last one was a compressed byte grab what it references
            Some(bytes(offset.i + 1))
          }
        }
        else {
          None
        }

        // add to our cache
        nameCache.put(cacheIndex, CachedName(name, next))

        readNames(bytes, offset, nameCache, name +: names)
      }
    }
  }

  def readFromCache(bytes: Array[Byte], offset: Offset, nameCache: NameCache): Seq[String] = {
    val index: Byte = offset.next(bytes)

    if (nameCache.contains(index)) {
      readFromCache(Some(index), nameCache)
    }
    else {
      // we missed the initial entry the first time around so try to read it again
      readNames(bytes, new Offset(index), nameCache)
    }
  }

  @tailrec
  def readFromCache(index: Option[Byte], nameCache: NameCache, names: List[String] = List()): Seq[String] = {
    index.flatMap(nameCache.get) match {
      case Some(CachedName(name, next)) => readFromCache(next, nameCache, name +: names)
      case None => names.reverse
    }
  }
}

trait Message {
  def transactionId: Int

  def bytes: Array[Byte] = ???
}

object QueryMessage {

}

class QueryMessage(override val transactionId: Int) extends Message {

}

class ResponseMessage(override val transactionId: Int) extends Message