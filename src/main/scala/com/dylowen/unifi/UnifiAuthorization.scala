package com.dylowen.unifi


import java.net.URL

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Cookie, HttpCookie, `Set-Cookie`}
import com.dylowen.nanoleaf.NanoSystem
import com.typesafe.scalalogging.LazyLogging

import scala.collection.immutable
import scala.concurrent.Future

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object UnifiAuthorization extends LazyLogging with UnifiLoginJsonSupport {

  private val SessionCookieName: String = "unifises"
  private val CsrfCookieName: String = "csrf_token"

  def apply()(implicit nanoSystem: NanoSystem): Future[UnifiAuthorization] = {
    val unifiConfig: UnifiConfig = UnifiConfig(nanoSystem.config)
      .getOrElse({
        throw new UnsupportedOperationException("Unifi config required")
      })

    loginRequest(unifiConfig)
  }

  def loginRequest(unifiConfig: UnifiConfig)
                  (implicit nanoSystem: NanoSystem): Future[UnifiAuthorization] = {
    import nanoSystem.{actorSystem, executionContext}

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = unifiConfig.baseUrl.toString + "/api/login",
      entity = UnifiLoginJson(unifiConfig.username, unifiConfig.password)
    )

    Http().singleRequest(request)
      .flatMap((response: HttpResponse) => {
        val cookies: Seq[HttpCookie] = response.headers.collect({
          case `Set-Cookie`(cookie) => cookie
        })

        val test = apply(cookies, unifiConfig)
          .map(Future.successful)
          .getOrElse(Future.failed(???))

        test
      })
  }

  def apply(cookies: Seq[HttpCookie], unifiConfig: UnifiConfig): Option[UnifiAuthorization] = {
    // make sure the cookies we care about exist
    for {
      sessionCookie <- cookies.find(_.name == SessionCookieName)
      csrfCookie <- cookies.find(_.name == CsrfCookieName)
    } yield {
      val other: Seq[HttpCookie] = cookies.filter((cookie: HttpCookie) => cookie.name != SessionCookieName && cookie.name != CsrfCookieName)

      UnifiAuthorization(sessionCookie, csrfCookie, other, unifiConfig)
    }
  }
}

case class UnifiAuthorization(sessionCookie: HttpCookie,
                              csrfCookie: HttpCookie,
                              other: Seq[HttpCookie],
                              backingConfig: UnifiConfig) extends UnifiConfig {

  def request(method: HttpMethod = HttpMethods.GET,
              path: String = "",
              headers: immutable.Seq[HttpHeader] = Nil,
              entity: RequestEntity = HttpEntity.Empty): HttpRequest = {
    HttpRequest(
      method = method,
      uri = baseUrl + path,
      headers = getAuthCookieHeader +: headers,
      entity = entity
    )
  }

  def getAuthCookieHeader: Cookie = {
    Cookie(sessionCookie.pair(), (csrfCookie +: other).map(_.pair()): _*)
  }

  override val baseUrl: URL = backingConfig.baseUrl

  override val username: String = backingConfig.username

  override val password: String = backingConfig.password
}
