package com.dylowen.mdns.record

import com.dylowen.utils.Binary
import com.dylowen.utils.Binary._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
sealed trait RecordType {

  val typeId: Int

  def bytes: Array[Byte]
}

object RecordType {

  abstract class RecordTypeImpl(override val typeId: Int) extends RecordType {
    override val bytes: Array[Byte] = shortToBytes(typeId)

    override def toString: String = {
      s"${getClass.getSimpleName}()"
    }
  }

  case object A extends RecordTypeImpl(0x0001)

  case object PTR extends RecordTypeImpl(0x000C)

  case object TXT extends RecordTypeImpl(0x0010)

  case object AAAA extends RecordTypeImpl(0x001C)

  case object SRC extends RecordTypeImpl(0x0021)

  case class UNKNOWN(override val typeId: Int) extends RecordTypeImpl(typeId)

  val types: Map[Int, RecordType] = Map(Seq(
    A, PTR, TXT, AAAA, SRC
  ).map((t: RecordTypeImpl) => t.typeId -> t): _*)

  def apply(bytes: Array[Byte]): RecordType = {
    val id: Int = Binary.bytesToShort(bytes)
    types.getOrElse(id, UNKNOWN(id))
  }
}
