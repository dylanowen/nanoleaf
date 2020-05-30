package com.dylowen.house

import com.dylowen.house.utils.NotNothing

import scala.reflect.ClassTag

object TestUtils {

  def ItName[C: NotNothing](implicit clazz: ClassTag[C]): String = {
    clazz.runtimeClass.getSimpleName
  }
}
