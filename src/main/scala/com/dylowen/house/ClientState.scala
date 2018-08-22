package com.dylowen.house

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.unifi.{WifiClient, GetClients, UnifiAuthorization}

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
/*
object ClientState {

  val lastSeenThreshold: FiniteDuration = 7 minutes

  def apply()(implicit system: NanoSystem): Flow[Any, Seq[Client], NotUsed] = {
    val myClients: immutable.Set[String] = system.config.getStringList("nanoleaf.my-clients").asScala.toSet

    Flow[Any]
      .mapAsync(1)((_: Any) => UnifiAuthorization())
      .mapAsync(1)(GetClients(_))
      .map((clients: Seq[Client]) => {
        // filter the clients by known clients and if they were online in the threshold time
        val lastSeenCutoff: Instant = Instant.now().minusMillis(lastSeenThreshold.toMillis)
        clients
          .filter((client: Client) => {
            myClients.contains(client.mac) && client.lastSeen.isAfter(lastSeenCutoff)
          })
        /*
        .filter((client: Client) => {
          client.ip.flatMap((ip: String) => {
              Try({
                // attempt to reach our ip address with a timeout of 200 ms
                InetAddress.getByName(ip).isReachable(200)
              }).toOption
            })
            .getOrElse(false)
        })
        */
      })
  }
}*/