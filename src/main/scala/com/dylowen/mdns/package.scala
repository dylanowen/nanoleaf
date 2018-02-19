package com.dylowen

import com.dylowen.utils.Binary
import com.dylowen.utils.Binary._

import scala.collection.mutable

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
package object mdns {

  type NameCache = mutable.Map[Byte, CachedName]

  /**
    * NOT Threadsafe or immutable
    *
    * Like an iterator but not as good
    */
  class Offset(var i: Int = 0) {
    def next[T](array: Array[T]): T = {
      val result: T = array(i)
      i += 1

      result
    }

    def peek[T](array: Array[T]): T = {
      array(i)
    }

    def slice[T](array: Array[T], length: Int): Array[T] = {
      val arr: Array[T] = array.slice(i, i + length)
      i += length

      arr
    }
  }

  def bytesToShort(bytes: Array[Byte], offset: Offset): Int = {
    val short: Int = Binary.bytesToShort(bytes, offset.i)
    offset.i += ShortBytes

    short
  }

  def bytesToInt(bytes: Array[Byte], offset: Offset): Int = {
    val int: Int = Binary.bytesToInt(bytes, offset.i)
    offset.i += IntBytes

    int
  }
}
