package com.dylowen.house
package control

import java.time.Instant

import com.dylowen.house.unifi.NetworkClient
import com.dylowen.house.utils._

import scala.concurrent.duration.{FiniteDuration, _}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object HouseState {

  def empty(system: HouseSystem): HouseState = {
    val lastSeenClientThreshold: FiniteDuration = system.config.getFiniteDuration("clients.offline-threshold")

    new HouseState(Set.empty, Set.empty, Set.empty, Set.empty, lastSeenClientThreshold)
  }

  object AtHomePhones {

    def unapply(state: HouseState): Option[Set[NetworkClient]] = {
      Some(state.atHomePhones)
    }
  }
}

class HouseState private[control] (
    private val phones: Set[NetworkClient],
    private val wirelessClients: Set[NetworkClient],
    private val lastPhones: Set[NetworkClient],
    private val lastUnknownWirelessClients: Set[NetworkClient],
    private val lastSeenClientThreshold: FiniteDuration,
    private val unknownClientThreshold: FiniteDuration = 14 days
) {

  lazy val atHomePhones: Set[NetworkClient] = {
    val lastSeenCutoff: Instant = getLastSeenCutoff

    // the phone is home if we've seen it since the cutoff time
    phones
      .filter(_.last_seen.isAfter(lastSeenCutoff))
  }

  lazy val arrivedHomePhones: Set[NetworkClient] = {
    val arrivedCutoff: Instant = getArrivedCutoff

    // if the phone is home, it didn't just arrive home, and has been gone double the threshold cutoff time
    atHomePhones.filter((client: NetworkClient) => {
      !lastPhones.find(_.mac == client.mac).exists(_.last_seen.isAfter(arrivedCutoff))
    })
  }

  lazy val atHomeUnknownWirelessClients: Set[NetworkClient] = {
    val lastSeenCutoff: Instant = getLastSeenCutoff
    val unknownCutoff: Instant = getUnknownCutoff

    // the client is home if we've seen it since the cutoff time and the first time we saw it was more recent than our
    // unknown cutoff
    wirelessClients.filter((client: NetworkClient) => {
      client.last_seen.isAfter(lastSeenCutoff) && client.first_seen.isAfter(unknownCutoff)
    })
  }

  lazy val arrivedUnknownWirelessClients: Set[NetworkClient] = {
    // the unknown client arrived if we haven't seen it recently (don't worry about reconnecting here since the idea
    // is to warn the user)
    atHomeUnknownWirelessClients.filter((client: NetworkClient) => {
      !lastUnknownWirelessClients
        .exists(_.mac == client.mac)
    })
  }

  def next(wifiClients: WifiClients): HouseState = {
    new HouseState(
      wifiClients.phones.to,
      wifiClients.wirelessClients.to,
      phones,
      atHomeUnknownWirelessClients,
      lastSeenClientThreshold
    )
  }

  override def toString: String = {
    s"${getClass.getSimpleName}(" +
      s"phones: ${atHomePhones.size} " +
      s"arrived phones: ${arrivedHomePhones.size} " +
      s"unknown clients: ${atHomeUnknownWirelessClients.size} Δ${atHomeUnknownWirelessClients.size - lastUnknownWirelessClients.size} " +
      s"arrived unknown clients: ${arrivedUnknownWirelessClients.size}" +
      s")"
  }

  // to prevent reconnecting, double the seen threshold for counting an arrival
  private def arrivedHomeThreshold: FiniteDuration = lastSeenClientThreshold * 2

  private def getLastSeenCutoff: Instant = Instant.now().minusMillis(lastSeenClientThreshold.toMillis)
  private def getArrivedCutoff: Instant = Instant.now().minusMillis(arrivedHomeThreshold.toMillis)
  private def getUnknownCutoff: Instant = Instant.now().minusMillis(unknownClientThreshold.toMillis)
}
