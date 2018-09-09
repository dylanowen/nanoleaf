package com.dylowen.house
package control

import java.time.Instant

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import com.dylowen.house.unifi.UnifiClient.UnifiRequest
import com.dylowen.house.unifi.{GetClients, UnifiClient, WifiClient}
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

  private case class SetWifiClients(response: Option[ActiveClients]) extends Msg
}

class WifiClientsWatcher(implicit system: HouseSystem) extends LazyLogging {

  import WifiClientsWatcher._

  private type UnifiResponse = Try[Either[ClientError, Seq[WifiClient]]]

  private val RefreshWifiClientsInterval: FiniteDuration = system.config.getFiniteDuration("unifi.refresh-interval")

  private val unifiClient: UnifiClient = new UnifiClient()

  val myPhones: Map[String, ClientConfig] = ClientConfig.configs(system.config.getConfig("clients.phones"))

  val lastSeenClientThreshold: FiniteDuration = system.config.getFiniteDuration("clients.offline-threshold")


  def behavior(parent: ActorRef[ActiveClients]): Behavior[Msg] = Behaviors.supervise(
    Behaviors.withTimers((timers: TimerScheduler[Msg]) => {
      Behaviors.setup((setup: ActorContext[Msg]) => {
        val unifiClientMapper: ActorRef[UnifiResponse] = setup
          .messageAdapter(mapClientResponse)

        val clientActor: ActorRef[UnifiClient.Msg] = setup.spawn(unifiClient.behavior, "unifi-client")

        // kick off our wifi client refresh
        setup.self ! RefreshWifiClients

        withState(parent, ActiveClients(), clientActor, unifiClientMapper, timers)
      })
    })
  ).onFailure(SupervisorStrategy.restart)

  private def withState(parent: ActorRef[ActiveClients],
                        clients: ActiveClients,
                        clientActor: ActorRef[UnifiClient.Msg],
                        clientMapper: ActorRef[UnifiResponse],
                        timers: TimerScheduler[Msg]): Behavior[Msg] = Behaviors.receivePartial({
    case (_, RefreshWifiClients) => {
      clientActor ! UnifiRequest(GetClients(_), clientMapper)

      Behaviors.same
    }
    case (_, SetWifiClients(response)) => {
      // schedule our next client refresh
      timers.startSingleTimer(RefreshWifiClientsInterval, RefreshWifiClients, RefreshWifiClientsInterval)

      val nextClients: ActiveClients = response
        .getOrElse({
          // if we didn't find any clients, filter our existing known ones by time
          filterCurrentClients(clients.phones ++ clients.wirelessClients)
        })

      // tell our parent about the current clients
      parent ! nextClients

      withState(parent, nextClients, clientActor, clientMapper, timers)
    }
  })

  private def mapClientResponse(response: UnifiResponse): Msg = {
    val mappedResponse: Option[ActiveClients] = response match {
      case Success(body) => body match {
        case Right(wifiClients) => {
          val nextClients: ActiveClients = filterCurrentClients(wifiClients)

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

  private def filterCurrentClients(clients: Seq[WifiClient]): ActiveClients = {
    // filter the clients by known clients and if they were online in the threshold time
    val lastSeenCutoff: Instant = Instant.now().minusMillis(lastSeenClientThreshold.toMillis)

    logger.debug(s"All clients: $clients")

    val recentClients: Seq[WifiClient] = clients
        .filter(_.last_seen.isAfter(lastSeenCutoff))

    val phones: Seq[WifiClient] = recentClients
      .filter((client: WifiClient) => {
        myPhones.contains(client.mac)
      })

    val wirelessClients: Seq[WifiClient] = recentClients
        .filterNot(_.is_wired)

    ActiveClients(phones, wirelessClients)
  }
}

