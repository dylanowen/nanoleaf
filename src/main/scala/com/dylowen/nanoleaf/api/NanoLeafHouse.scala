package com.dylowen.nanoleaf.api

import java.net.InetAddress

import com.dylowen.nanoleaf.NanoSystem

import scala.concurrent.Future
import scala.util.Try

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
class NanoLeafHouse()(implicit nanoSystem: NanoSystem) {

  import nanoSystem.executionContext

  def getClients: Future[NanoLeafClient] = {
    /*
    mdns.query
      .map((instances: Set[Instance]) => {
        // filter our instance by ones we have auth for
        instances.collect(Function.unlift((instance: Instance) => {
          // see if we have an id attribute
          Option(instance.lookupAttribute("id"))
            .flatMap((id: String) => {
              Try({
                (id, nanoSystem.config.getString(s"""nanoleaf.device-auth."$id""""))
              }).toOption
            })
            .map(((id: String, auth: String) => {
              NanoLeafClient(instance.getName, instance.getAddresses.asScala.toSet, instance.getPort, id, auth)
            }).tupled)
        }))
      })
      .map(_.head) // grab the first one since we don't support multiple
      */
    Future.successful(Option("F5:A3:8F:28:75:C1")
      .flatMap((id: String) => {
        Try({
          (id, nanoSystem.config.getString(s"""nanoleaf.device-auth."$id""""))
        }).toOption
      })
      .map(((id: String, auth: String) => {
        NanoLeafClient("nano", Set(InetAddress.getByName("192.168.1.229")), 16021, id, auth)
      }).tupled)
      .get)
  }
}
