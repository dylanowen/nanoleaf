package com.dylowen.house
package unifi

import com.dylowen.house.utils.{ClientConfig, ClientError}
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.{DeserializationError, Response, SttpBackend}
import com.typesafe.scalalogging.LazyLogging
import io.circe
import io.circe.generic.auto._

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object GetClients extends WifiClientJsonSupport with LazyLogging {

  def apply(auth: UnifiAuthorization, site: String = "default")(
      implicit nanoSystem: HouseSystem
  ): Future[Either[ClientError, Seq[NetworkClient]]] = {
    import nanoSystem.executionContext

    implicit val backend: SttpBackend[Future, Nothing] = ClientConfig.backend

    auth
      .request(path = s"/api/s/$site/stat/sta")
      .response(asJson[UnifiRPCJson])
      .send()
      .map((response: Response[Either[DeserializationError[circe.Error], UnifiRPCJson]]) => {
        response.body match {
          case Right(body) => {
            body.left
              .map(_.error)
              .flatMap(_.data.as[Seq[NetworkClient]])
              .left
              .map((error: circe.Error) => {
                ClientError(
                  message = Some("Parsing failure: " + error.toString),
                  response = Some(response)
                )
              })
          }
          case Left(error) =>
            Left(
              ClientError(
                message = Some("Request error: " + error.toString),
                response = Some(response)
              )
            )
        }
      })
      .recoverWith({
        case NonFatal(t) => {
          Future.successful(Left(ClientError(message = Some("Request failed"), throwable = Some(t))))
        }
      })
  }
}
