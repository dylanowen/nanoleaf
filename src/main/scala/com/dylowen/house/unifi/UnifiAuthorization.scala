package com.dylowen.house
package unifi


import java.net.URL
import java.time.ZonedDateTime

import com.dylowen.house.utils.{ClientConfig, ClientError}
import com.softwaremill.sttp.circe._
import com.softwaremill.sttp.{Cookie, SttpBackend, sttp, _}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object UnifiAuthorization extends LazyLogging {

  private val DefaultAuthExpiration: FiniteDuration = 30 minutes

  private val SessionCookieName: String = "unifises"
  private val CsrfCookieName: String = "csrf_token"

  private case class UnifiLoginJson(username: String, password: String)

  def apply()(implicit nanoSystem: HouseSystem): Future[Either[ClientError, UnifiAuthorization]] = {
    val unifiConfig: UnifiConfig = UnifiConfig(nanoSystem.config)
      .getOrElse({
        throw new UnsupportedOperationException("Unifi config required")
      })

    loginRequest(unifiConfig)
  }

  def loginRequest(unifiConfig: UnifiConfig)
                  (implicit nanoSystem: HouseSystem): Future[Either[ClientError, UnifiAuthorization]] = {
    import nanoSystem.executionContext

    implicit val backend: SttpBackend[Future, Nothing] = ClientConfig.backend

    sttp.post(uri"${unifiConfig.baseUrl.toString}/api/login")
      .body(UnifiLoginJson(unifiConfig.username, unifiConfig.password))
      .send()
      .map((response: Response[String]) => {
        // verify we have both cookies we need
        val foundCookies: Option[Option[ZonedDateTime]] = for {
          sessionCookie <- response.cookies.find(_.name == SessionCookieName)
          _ <- response.cookies.find(_.name == CsrfCookieName)
        } yield sessionCookie.expires

        foundCookies
          .map((maybeExpiration: Option[ZonedDateTime]) => {
            val expiration: FiniteDuration = maybeExpiration
              .map((expiration: ZonedDateTime) => {
                (expiration.toInstant.toEpochMilli - System.currentTimeMillis()) milliseconds
              })
              .getOrElse(DefaultAuthExpiration)

            Right(apply(unifiConfig.username, expiration, response.cookies, unifiConfig.baseUrl))
          })
          .getOrElse({
            Left(ClientError(
              message = Some("Couldn't find the necessary cookies after login"),
              response = Some(response)
            ))
          })
      })
  }
}

case class UnifiAuthorization(username: String,
                              expiration: FiniteDuration,
                              cookies: immutable.Seq[Cookie],
                              baseUrl: URL) {

  def request(method: Method = Method.GET,
              path: String = "",
              headers: immutable.Seq[(String, String)] = immutable.Seq()): Request[String, Nothing] = {
    val rawUri: String = s"${baseUrl.toString}$path"

    sttp
      .copy[Id, String, Nothing](uri = uri"$rawUri", method = method)
      .cookies(cookies)
      .headers(headers: _*)
  }
}
