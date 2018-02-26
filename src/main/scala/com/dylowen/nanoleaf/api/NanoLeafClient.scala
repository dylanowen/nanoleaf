package com.dylowen.nanoleaf.api

import java.net.{Inet4Address, Inet6Address, InetAddress}

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.{Authority, Host, Path}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.utils.UtilJsonSupport
import spray.json.RootJsonFormat

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class NanoLeafClient(name: String, addresses: immutable.Set[InetAddress], port: Int, id: String, auth: String)
                         (implicit nanoSystem: NanoSystem) extends UtilJsonSupport {

  import nanoSystem.{actorSystem, executionContext, materializer}

  final case class StateOnWrapper(on: StateOn)

  final case class StateOn(value: Boolean)

  final case class StateBrightnessWrapper(brightness: StateBrightness)

  final case class StateBrightness(value: Int, duration: Option[Int])

  implicit val StateOnJsonFormat: RootJsonFormat[StateOn] = jsonFormat1(StateOn)

  implicit val StateOnWrapperJsonFormat: RootJsonFormat[StateOnWrapper] = jsonFormat1(StateOnWrapper)

  implicit val StateBrightnessJsonFormat: RootJsonFormat[StateBrightness] = jsonFormat2(StateBrightness)

  implicit val StateBrightnessWrapperJsonFormat: RootJsonFormat[StateBrightnessWrapper] = jsonFormat1(StateBrightnessWrapper)

  assert(addresses.nonEmpty)

  def isOn: Future[Boolean] = {
    val request: HttpRequest = HttpRequest(uri = getUri("/api/v1/<auth>/state/on"))

    Http().singleRequest(request)
      .flatMap((response: HttpResponse) => {
        Unmarshal(response.entity).to[StateOn]
      })
      .map(_.value)
  }

  def brightness: Future[Int] = {
    val request: HttpRequest = HttpRequest(uri = getUri("/api/v1/<auth>/state/brightness"))

    Http().singleRequest(request)
      .flatMap((response: HttpResponse) => {
        Unmarshal(response.entity).to[StateBrightness]
      })
      .map(_.value)
  }

  def setBrightness(brightness: Int, duration: Option[FiniteDuration] = None): Future[Unit] = {
    val durationSeconds: Option[Int] = duration.map(_.toSeconds.toInt)

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.PUT,
      uri = getUri("/api/v1/<auth>/state"),
      entity = StateBrightnessWrapper(StateBrightness(brightness, durationSeconds))
    )

    Http().singleRequest(request)
      .map((_: HttpResponse) => (): Unit)
  }

  private def getUri(path: String, preferIpv6: Boolean = false): Uri = {
    val address: InetAddress = addresses.collectFirst({
      case i: Inet6Address if preferIpv6 => i
      case i: Inet4Address if !preferIpv6 => i
    })
      .getOrElse(addresses.head)

    Uri(
      scheme = "http",
      authority = Authority(Host(address), port),
      path = Path(path.replace("<auth>", auth))
    )
  }
}
