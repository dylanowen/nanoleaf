package com.dylowen.house.nanoleaf

import com.dylowen.house.HouseSystem
import com.dylowen.house.nanoleaf.api.NanoleafClient
import com.dylowen.house.utils.ClientError
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

/**
  * A reseponse handler that just logs the error
  *
  * @author dylan.owen
  * @since Sep-2018
  */
trait ResponseHandler {
  this: LazyLogging =>

  implicit val system: HouseSystem

  import system.executionContext

  protected def handleResponse[T](response: Future[Either[ClientError, T]],
                                  client: NanoleafClient): Future[Option[T]] = {
    response
      .map({
        case Right(t) => Some(t)
        case Left(error) => {
          error.logError(s"client error", logger)

          None
        }
      })
  }
}
