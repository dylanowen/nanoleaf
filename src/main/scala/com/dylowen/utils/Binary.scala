package com.dylowen.utils

import scala.language.implicitConversions

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object Binary {
  implicit def intToByte(int: Int): Byte = int.toByte

  val ByteMask: Int = 0xFF

  val ShortMask: Int = 0xFFFF

  val ByteBits: Int = java.lang.Byte.SIZE
  val ShortBits: Int = java.lang.Short.SIZE
  val IntBits: Int = java.lang.Integer.SIZE

  val ByteBytes: Int = 1
  val ShortBytes: Int = ShortBits / ByteBits
  val IntBytes: Int = IntBits / ByteBits


  def shortToBytes(id: Int): Array[Byte] = {
    val left: Byte = id >> ByteBits & ByteMask
    val right: Byte = id & ByteMask

    Array(left, right)
  }

  def bytesToShort(bytes: Array[Byte], offset: Int = 0): Int = {
    bytes(offset).demote << ByteBits |
      bytes(offset + 1).demote
  }

  def bytesToInt(bytes: Array[Byte], offset: Int = 0): Int = {
    bytes(0).demote << (ByteBits * 3) |
      bytes(offset + 1).demote << (ByteBits * 2) |
      bytes(offset + 2).demote << (ByteBits * 1) |
      bytes(offset + 3).demote
  }

  /**
    * Java promotes bytes to integers which is super annoying for bitwise manipulation.
    * This promotes the byte, then strips out the Int garbage
    */
  def demoteByte(byte: Byte): Int = byte.toInt & ByteMask

  implicit class EnhancedByte(underlying: Byte) {

    def demote: Int = {
      demoteByte(underlying)
    }
  }

}
