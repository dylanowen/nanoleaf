package com.dylowen.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.RequestEntity
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.language.implicitConversions

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
trait UtilJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def obj2RequestEntity[T](obj: T)
                                   (implicit jsonFormat: RootJsonFormat[T]): RequestEntity = {
    jsonFormat.write(obj).compactPrint
  }
}
