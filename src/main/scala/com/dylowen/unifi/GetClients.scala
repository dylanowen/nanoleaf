package com.dylowen.unifi

import com.dylowen.nanoleaf.NanoSystem
import com.softwaremill.sttp.circe.asJson
import com.softwaremill.sttp.{Response, SttpBackend}
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
object GetClients extends WifiClientJsonSupport {

  def apply(auth: UnifiAuthorization, site: String = "default")
           (implicit nanoSystem: NanoSystem): Future[Either[UnifiClientError, Seq[WifiClient]]] = {
    import nanoSystem.executionContext

    implicit val backend: SttpBackend[Future, Nothing] = UnifiClientBackend

    auth.request(path = s"/api/s/$site/stat/sta")
      .response(asJson[UnifiRPCJson])
      .send()
      .map((response: Response[Either[circe.Error, UnifiRPCJson]]) => {
        response.body match {
          case Right(body) => {
            body
              .flatMap(_.data.as[Seq[WifiClient]])
              .left
              .map((error: circe.Error) => {
                UnifiClientError(
                  message = Some("Parsing failure: " + error.toString),
                  response = Some(response)
                )
              })
          }
          case Left(error) => Left(UnifiClientError(
            message = Some("Request error: " + error.toString),
            response = Some(response)
          ))
        }
      })
      .recoverWith({
        case NonFatal(t) => {
          Future.successful(Left(UnifiClientError(
            message = Some("Request failed"),
            throwable = Some(t))
          ))
        }
      })
  }

  /*
  Http().singleRequest(request)
    .flatMap((response: HttpResponse) => {
      Unmarshal(response.entity).to[UnifiRPCJson]
    })
    .map((rpcJson: UnifiRPCJson) => {
      rpcJson.data match {
        case JsArray(elements) => elements.map(_.convertTo[Client])
        case _ => Json.deserializationError("expected an array")
      }
    })
*/
}


