package com.dylowen.house
package nanoleaf.api


import com.dylowen.house.nanoleaf.mdns.NanoleafAddress
import com.dylowen.house.utils.{ClientConfig, ClientError}
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp.{Id, SttpBackend, sttp, _}
import com.typesafe.scalalogging.LazyLogging
import io.circe
import io.circe.generic.auto._

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

/**
  * TODO add description
  *
  * @author dylan.owen¬
  * @since Jan-2018
  */
object NanoleafClient {
  final case class StateOnWrapper(on: StateOn)

  final case class StateOn(value: Boolean)

  final case class StateBrightnessWrapper(brightness: StateBrightness)

  final case class StateBrightness(value: Int, duration: Option[Int])

}

case class NanoleafClient(address: NanoleafAddress, auth: String)
                         (implicit nanoSystem: HouseSystem) extends LazyLogging {

  import NanoleafClient._
  import nanoSystem.executionContext

  implicit val backend: SttpBackend[Future, Nothing] = ClientConfig.backend

  def isOn: Future[Either[ClientError, Boolean]] = {
    request(path = "/api/v1/<auth>/state/on")
      .response(asJson[StateOn])
      .send()
      .clientMapError
      .map((response: Either[ClientError, StateOn]) => {
        response.map(_.value)
      })
  }

  def brightness: Future[Either[ClientError, Brightness]] = {
    request(path = "/api/v1/<auth>/state/brightness")
      .response(asJson[Brightness])
      .send()
      .clientMapError
  }


  def setBrightness(brightness: Int, duration: Option[FiniteDuration] = None): Future[Either[ClientError, Unit]] = {
    val durationSeconds: Option[Int] = duration.map(_.toSeconds.toInt)

    request(Method.PUT, "/api/v1/<auth>/state")
      .body(StateBrightnessWrapper(StateBrightness(brightness, durationSeconds)))
      .send()
      .map(mapIgnoredResponse)
      .clientRecover
  }

  def setState(on: Boolean): Future[Either[ClientError, Unit]] = {
    request(Method.PUT, "/api/v1/<auth>/state")
      .body(StateOnWrapper(StateOn(on)))
      .send()
      .map(mapIgnoredResponse)
      .clientRecover
  }

  def tempDisplay(effect: EffectCommand): Future[Either[ClientError, Unit]] = {
    request(Method.PUT, "/api/v1/<auth>/effects")
      .body(WriteWrapper(effect.copy(command = "displayTemp")))
      .send()
      .map(mapIgnoredResponse)
      .clientRecover
  }

  def identify: Future[Either[ClientError, Unit]] = {
    request(Method.PUT, "/api/v1/<auth>/identify")
      .send()
      .map(mapIgnoredResponse)
      .clientRecover
  }

  private def mapIgnoredResponse(response: Response[String]): Either[ClientError, Unit] = {
    response.body
      .map((_: String) => (): Unit)
      .left
      .map((errorBody: String) => {
        ClientError(
          message = Some(s"Error: $errorBody"),
          response = Some(response)
        )
      })
  }

  private def request(method: Method = Method.GET, path: String): Request[String, Nothing] = {
    val rawUri: String = address.url + path.replace("<auth>", auth)

    sttp
      .copy[Id, String, Nothing](uri = uri"$rawUri", method = method)
  }

  implicit class EnhancedFuture[T](future: Future[Either[ClientError, T]]) {
    def clientRecover: Future[Either[ClientError, T]] = {
      future.recoverWith({
        case NonFatal(t) => {
          Future.successful(Left(ClientError(
            message = Some("Request failed"),
            throwable = Some(t))
          ))
        }
      })
    }
  }

  implicit class EnhancedParseFuture[T](future: Future[Response[Either[circe.Error, T]]]) {
    def clientMapError: Future[Either[ClientError, T]] = {
      future.map((response: Response[Either[circe.Error, T]]) => {
        response.body match {
          case Right(body) => {
            body
              .left
              .map((error: circe.Error) => {
                ClientError(
                  message = Some("Parsing failure: " + error.toString),
                  response = Some(response)
                )
              })
          }
          case Left(error) => Left(ClientError(
            message = Some("Request error: " + error.toString),
            response = Some(response)
          ))
        }
      })
        .clientRecover
    }
  }

}