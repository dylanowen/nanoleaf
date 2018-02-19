package com.dylowen.mdns

import com.dylowen.utils.Binary._

import scala.collection.mutable

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object Query {

  def apply(bytes: Array[Byte], offset: Offset, nameCache: NameCache, numberOfQueries: Int): Seq[Query] = {
    val builder: mutable.Builder[Query, Seq[Query]] = Seq.newBuilder

    for (_ <- 0 until numberOfQueries) {
      builder += apply(bytes, offset, nameCache)
    }

    builder.result()
  }

  def apply(bytes: Array[Byte], offset: Offset, nameCache: NameCache): Query = {
    val names: Seq[String] = Message.readNames(bytes, offset, nameCache)

    val queryType: Int = bytesToShort(bytes, offset)

    val responseType: Int = bytesToShort(bytes, offset)
    val unicastResponse: Boolean = (responseType >> (ShortBits - 1)) == 0x01
    val queryClass: Int = responseType & (ShortMask >> 1)

    Query(names, queryType, unicastResponse, queryClass)
  }
}

case class Query(names: Seq[String],
                 queryType: Int,
                 unicastResponse: Boolean,
                 queryClass: Int) {

}
