package com.dylowen.unifi

import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.{BodySerializer, Method, Request, Response, SttpBackend}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object UnifiClient {
  private implicit val backend: SttpBackend[Future, Nothing] = AsyncHttpClientFutureBackend()

  /*
  def request[B: BodySerializer](auth: UnifiAuthorization,
                                 method: Method = Method.GET,
                                 path: String = "",
                                 headers: immutable.Seq[(String, String)] = immutable.Seq(),
                                 body: Option[B] = None): Request[String, Nothing] = {
    auth.request(method, path, headers, body)
  }

  def send[R, B: BodySerializer](auth: UnifiAuthorization,
                                 method: Method = Method.GET,
                                 path: String = "",
                                 headers: immutable.Seq[(String, String)] = immutable.Seq(),
                                 body: Option[B] = None)
                                (implicit ec: ExecutionContext): Future[Either[UnifiClientError, R]] = {
    send[R](request(method, path, headers, body, auth))
  }

  def send[R](request: Request[R, Nothing])
             (implicit ec: ExecutionContext): Future[Either[UnifiClientError, R]] = {
    request.send()
      .map((response: Response[R]) => {
        response.body match {
          case Right(body) => Right(body)
          case Left(error) => Left(UnifiClientError(
            message = Some("Request error: " + error.toString),
            response = Some(response)
          ))
        }
      })
      .recoverWith({
        case NonFatal(t) => {
          Future.successful(Left(UnifiClientError(message = Some("Request failed"), throwable = Some(t))))
        }
      })
  }

*/
}
