package com.dylowen.house
package control

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import com.dylowen.house.nanoleaf.NanoleafDispatcher
import com.dylowen.house.utils._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.FiniteDuration

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object HouseControl {

  /**
    * Actor API
    */
  sealed trait Msg

  private case object Tick extends Msg

  private case class SetWifiClients(response: ActiveClients) extends Msg

}

class HouseControl(implicit system: HouseSystem) extends LazyLogging {

  import HouseControl._

  private val TickInterval: FiniteDuration = system.config.getFiniteDuration("nanoleaf.update-interval")

  private val nanoleafDispatcher: NanoleafDispatcher = new NanoleafDispatcher()
  private val clientsWatcher: WifiClientsWatcher = new WifiClientsWatcher()

  val behavior: Behavior[Msg] = Behaviors.supervise(
    Behaviors.withTimers((timers: TimerScheduler[Msg]) => {
      timers.startPeriodicTimer(TickInterval, Tick, TickInterval)

      Behaviors.setup((setup: ActorContext[Msg]) => {
        val clientsMapper: ActorRef[ActiveClients] = setup
          .messageAdapter(SetWifiClients)

        // create our clients watcher
        setup.spawn(clientsWatcher.behavior(clientsMapper), "unifi-clients-watcher")

        // create our nanoleaf dispatcher
        val nanoleaf: ActorRef[NanoleafDispatcher.Msg] = setup.spawn(nanoleafDispatcher.behavior, "nanoleaf-dispatcher")

        def withState(clients: ActiveClients, lastClients: ActiveClients): Behavior[Msg] = Behaviors.receiveMessagePartial({
          case Tick => {
            val state: HouseState = HouseState(
              clients.phones,
              clients.wirelessClients,
              lastClients.phones,
              lastClients.wirelessClients
            )

            logger.debug(s"House State: $state")

            nanoleaf ! NanoleafDispatcher.Tick(state)

            Behaviors.same
          }
          case SetWifiClients(nextClients) => {
            withState(nextClients, clients)
          }
        })

        withState(ActiveClients(), ActiveClients())
      })
    })
  ).onFailure(SupervisorStrategy.restart)
}
