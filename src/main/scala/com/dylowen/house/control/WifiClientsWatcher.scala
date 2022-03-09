package com.dylowen.house
package control

import java.time.Instant

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import com.dylowen.house.unifi.UnifiClient.UnifiRequest
import com.dylowen.house.unifi.{GetClients, NetworkClient, UnifiClient}
import com.dylowen.house.utils.{ClientError, _}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object WifiClientsWatcher {

  /**
    * Actor API
    */
  sealed trait Msg

  private case object RefreshWifiClients extends Msg

  private case class SetWifiClients(response: Option[WifiClients]) extends Msg
}

class WifiClientsWatcher(implicit system: HouseSystem) extends LazyLogging {

  import WifiClientsWatcher._

  private type UnifiResponse = Try[Either[ClientError, Seq[NetworkClient]]]

  private val RefreshWifiClientsInterval: FiniteDuration = system.config.getFiniteDuration("unifi.refresh-interval")

  private val unifiClient: UnifiClient = new UnifiClient()

  val myPhones: Map[String, ClientConfig] = ClientConfig.configs(system.config.getConfig("clients.phones"))

  def behavior(parent: ActorRef[WifiClients]): Behavior[Msg] =
    Behaviors
      .supervise(
        Behaviors.withTimers((timers: TimerScheduler[Msg]) => {
          Behaviors.setup((setup: ActorContext[Msg]) => {
            val unifiClientMapper: ActorRef[UnifiResponse] = setup
              .messageAdapter(mapClientResponse)

            val clientActor: ActorRef[UnifiClient.Msg] = setup.spawn(unifiClient.behavior, "unifi-client")

            // kick off our wifi client refresh
            setup.self ! RefreshWifiClients

            withState(parent, WifiClients(), clientActor, unifiClientMapper, timers)
          })
        })
      )
      .onFailure(SupervisorStrategy.restart)

  private def withState(
      parent: ActorRef[WifiClients],
      clients: WifiClients,
      clientActor: ActorRef[UnifiClient.Msg],
      clientMapper: ActorRef[UnifiResponse],
      timers: TimerScheduler[Msg]
  ): Behavior[Msg] =
    Behaviors.receivePartial({
      case (_, RefreshWifiClients) => {
        clientActor ! UnifiRequest(GetClients(_), clientMapper)

        Behaviors.same
      }
      case (_, SetWifiClients(wifiClientsResponse)) => {
        // schedule our next client refresh
        timers.startSingleTimer(RefreshWifiClientsInterval, RefreshWifiClients, RefreshWifiClientsInterval)

        val nextClients: WifiClients = wifiClientsResponse
          .map({
            case WifiClients(nextPhones, nextWirelessClients) => clients.update(nextPhones, nextWirelessClients)
          })
          .getOrElse({
            logger.debug("Didn't find any Unifi Clients")
            // if we didn't find any clients, reuse our old ones
            clients
          })

        logger.debug(s"Filtered Unifi Clients: $nextClients")

        // tell our parent about the current clients
        parent ! nextClients

        withState(parent, nextClients, clientActor, clientMapper, timers)
      }
    })

  private def mapClientResponse(response: UnifiResponse): Msg = {
    val mappedResponse: Option[WifiClients] = response match {
      case Success(body) =>
        body match {
          case Right(wifiClients) => {
            val nextClients: WifiClients = filterWifiClients(wifiClients)

            Some(nextClients)
          }
          case Left(error) => {
            error.logError("Unifi client error", logger)

            None
          }
        }
      case Failure(exception) => {
        logger.error("Unifi client exception", exception)

        None
      }
    }

    SetWifiClients(mappedResponse)
  }

  private def filterWifiClients(clients: Seq[NetworkClient]): WifiClients = {
    logger.trace(s"All clients: $clients")

    // filter the clients by known clients
    val (phones, wirelessClients) = clients
      .filterNot(_.is_wired)
      .partition((client: NetworkClient) => {
        myPhones.contains(client.mac)
      })

    WifiClients(phones, wirelessClients)
  }
}
