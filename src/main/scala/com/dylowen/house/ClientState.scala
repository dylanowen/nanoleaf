package com.dylowen.house

import java.net.InetAddress

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.unifi.{Client, GetClients, UnifiAuthorization}

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.language.postfixOps
import scala.util.Try

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object ClientState {

  def apply()(implicit system: NanoSystem): Flow[Any, Seq[Client], NotUsed] = {
    val myClients: immutable.Set[String] = system.config.getStringList("nanoleaf.my-clients").asScala.toSet

    Flow[Any]
      .mapAsync(1)((_: Any) => UnifiAuthorization())
      .mapAsync(1)(GetClients.apply(_))
      .map((clients: Seq[Client]) => {
        // filter the clients by known clients and if they're reachable
        clients
          .filter((client: Client) => myClients.contains(client.mac))
          .filter((client: Client) => {
            client.ip.flatMap((ip: String) => {
                Try({
                  // attempt to reach our ip address with a timeout of 200 ms
                  InetAddress.getByName(ip).isReachable(200)
                }).toOption
              })
              .getOrElse(false)
          })
      })
  }
}