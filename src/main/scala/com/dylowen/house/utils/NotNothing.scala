package com.dylowen.house.utils

import scala.annotation.implicitNotFound

/**
  * Prevent inferring [[Nothing]]
  *
  * https://riptutorial.com/scala/example/21134/preventing-inferring-nothing
  */
@implicitNotFound("Nothing was inferred")
sealed trait NotNothing[-T]

object NotNothing {

  implicit object AnythingButNothing extends NotNothing[Any]

  // We do not want Nothing to be inferred, so make an ambiguous implicit
  implicit object AmbiguousNothing extends NotNothing[Nothing]
}
