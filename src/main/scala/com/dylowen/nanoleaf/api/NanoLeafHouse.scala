package com.dylowen.nanoleaf.api

import com.dylowen.mdns.MDnsService
import com.dylowen.nanoleaf.NanoSystem
import net.straylightlabs.hola.sd.Instance

import scala.collection.JavaConverters._
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

  val mdns: MDnsService = new MDnsService("_nanoleafapi._tcp")

  def getClients: Future[NanoLeafClient] = {
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
  }
}
