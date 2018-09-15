package com.dylowen.house
package nanoleaf

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.dylowen.house.control.HouseState
import com.dylowen.house.nanoleaf.mdns.{NanoleafAddress, NanoleafMdnsService}
import com.typesafe.scalalogging.LazyLogging

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object NanoleafDispatcher {

  /**
    * Actor API
    */
  trait Msg

  case class Tick(state: HouseState) extends Msg

  private case class DiscoveredLights(lights: Map[String, NanoleafAddress]) extends Msg

}

class NanoleafDispatcher(implicit system: HouseSystem) extends LazyLogging {

  import NanoleafDispatcher._

  private val nanoleafControl: NanoleafControl = new NanoleafControl()

  val behavior: Behavior[Msg] = Behaviors.setup((setup: ActorContext[Msg]) => {
    val lightAddressMapper: ActorRef[Map[String, NanoleafAddress]] = setup
      .messageAdapter(DiscoveredLights)

    val lightDiscovery: ActorRef[NanoleafMdnsService.Msg] = setup.spawn(NanoleafMdnsService.behavior, "nanoleaf-mdns-service")

    // tell the light discovery about us
    lightDiscovery ! NanoleafMdnsService.RegisterListener(lightAddressMapper)

    withState(Map())
  })

  private def withState(lights: Map[String, ActorRef[NanoleafControl.Msg]]): Behavior[Msg] = Behaviors.receivePartial({
    case (_, Tick(houseState)) => {
      // tick for all of our lights
      lights.values.foreach(_ ! NanoleafControl.Tick(houseState))

      Behaviors.same
    }
    case (ctx, DiscoveredLights(nextLights)) => {
      val added: Map[String, NanoleafAddress] = nextLights.filterKeys(!lights.contains(_))
      val (kept, removed) = lights.partition((entry: (String, ActorRef[NanoleafControl.Msg])) => {
        nextLights.contains(entry._1)
      })

      // stop our removed lights
      removed
        .values
        .foreach(ctx.stop)

      if (added.nonEmpty) {
        logger.info(s"Added Nanoleaf Lights: ${added.values}")
      }
      if (removed.nonEmpty) {
        logger.info(s"Removed Nanoleaf Lights: ${removed.keys}")
      }

      // start our added lights
      val started: Map[String, ActorRef[NanoleafControl.Msg]] = added
        .map((entry: (String, NanoleafAddress)) => {
          val address: NanoleafAddress = entry._2
          val child: ActorRef[NanoleafControl.Msg] = ctx.spawn(nanoleafControl.behavior(address), s"nanoleaf-control-${address.id}")

          entry._1 -> child
        })

      withState(kept ++ started)
    }
  })
}