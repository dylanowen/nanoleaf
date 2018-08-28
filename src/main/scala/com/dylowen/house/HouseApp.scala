package com.dylowen.house

import akka.actor.typed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.dylowen.house.control.HouseControl
import com.typesafe.scalalogging.LazyLogging

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object HouseApp extends LazyLogging {

  private case class BootstrapStart(system: HouseSystem)

  def main(args: Array[String]): Unit = {
    implicit val actorSystem: typed.ActorSystem[BootstrapStart] = typed.ActorSystem(bootstrap(false), "HouseManager")

    val system: HouseSystem = HouseSystem(actorSystem)

    // start our house
    actorSystem ! BootstrapStart(system)
  }

  private def bootstrap(started: Boolean): Behavior[BootstrapStart] = Behaviors.receivePartial({
    case (ctx, BootstrapStart(system)) => {
      if (!started) {
        implicit val houseSystem: HouseSystem = system
        val houseControl: HouseControl = new HouseControl()

        ctx.spawn(houseControl.behavior, "house-control")

        Behaviors.same
      }
      else {
        throw new RuntimeException("Can't start the house multiple times")
      }
    }
  })
}
