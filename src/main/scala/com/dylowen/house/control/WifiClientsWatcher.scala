package com.dylowen.house
package control

import java.time.Instant

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import com.dylowen.house.unifi.UnifiClient.UnifiRequest
import com.dylowen.house.unifi.{GetClients, UnifiClient, WifiClient}
import com.dylowen.house.utils.{ClientError, _}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.duration.{FiniteDuration, _}
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

  private case class SetWifiClients(response: Option[Seq[WifiClient]]) extends Msg

}

class WifiClientsWatcher(implicit system: HouseSystem) extends LazyLogging {

  import WifiClientsWatcher._

  private type UnifiResponse = Try[Either[ClientError, Seq[WifiClient]]]

  private val RefreshWifiClientsInterval: FiniteDuration = system.config.getFiniteDuration("unifi.refresh-interval")

  private val unifiClient: UnifiClient = new UnifiClient()

  val myClients: immutable.Set[String] = system.config.getStringList("unifi.clients.phones").asScala.toSet
  val lastSeenClientThreshold: FiniteDuration = system.config.getFiniteDuration("unifi.clients.offline-threshold")


  def behavior(parent: ActorRef[Seq[WifiClient]]): Behavior[Msg] = Behaviors.supervise(
    Behaviors.withTimers((timers: TimerScheduler[Msg]) => {
      Behaviors.setup((setup: ActorContext[Msg]) => {
        val unifiClientMapper: ActorRef[UnifiResponse] = setup
          .messageAdapter(mapClientResponse)

        val clientActor: ActorRef[UnifiClient.Msg] = setup.spawn(unifiClient.behavior, "unifi-client")

        // kick off our wifi client refresh
        setup.self ! RefreshWifiClients

        withState(parent, Seq(), clientActor, unifiClientMapper, timers)
      })
    })
  ).onFailure(SupervisorStrategy.restart)

  private def withState(parent: ActorRef[Seq[WifiClient]],
                        clients: Seq[WifiClient],
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

      val nextClients: Seq[WifiClient] = response
        .getOrElse({
          // if we didn't find any clients, filter our existing known ones by time
          filterKnownCurrentClients(clients)
        })

      // tell our parent about the current clients
      parent ! nextClients

      withState(parent, nextClients, clientActor, clientMapper, timers)
    }
  })

  private def mapClientResponse(response: UnifiResponse): Msg = {
    val mappedResponse: Option[Seq[WifiClient]] = response match {
      case Success(body) => body match {
        case Right(wifiClients) => {
          val nextClients: Seq[WifiClient] = filterKnownCurrentClients(wifiClients)

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

  private def filterKnownCurrentClients(clients: Seq[WifiClient]): Seq[WifiClient] = {
    // filter the clients by known clients and if they were online in the threshold time
    val lastSeenCutoff: Instant = Instant.now().minusMillis(lastSeenClientThreshold.toMillis)

    logger.debug(s"All clients: $clients")

    clients.filter((client: WifiClient) => {
      myClients.contains(client.mac) && client.`last_seen`.isAfter(lastSeenCutoff)
    })
  }
}

