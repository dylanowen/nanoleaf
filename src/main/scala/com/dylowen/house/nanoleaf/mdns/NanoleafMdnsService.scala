package com.dylowen.house
package nanoleaf.mdns

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, PostStop, SupervisorStrategy}
import com.typesafe.scalalogging.LazyLogging
import javax.jmdns
import javax.jmdns.ServiceInfo

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.matching.Regex

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object NanoleafMdnsService extends LazyLogging {

  /**
    * Actor API
    */
  sealed trait Msg

  private[mdns] case class ServiceEvent(event: jmdns.ServiceEvent) extends Msg

  private case object RefreshNetworkInterfaces extends Msg

  case class RegisterListener(ref: ActorRef[Map[String, NanoleafAddress]]) extends Msg

  case class GetLights(ref: ActorRef[Map[String, NanoleafAddress]]) extends Msg


  private val InterfaceRefreshInterval: FiniteDuration = 30 minutes

  private[nanoleaf] val NanoleafServiceType: String = "_nanoleafapi._tcp.local."
  private val NameRegex: Regex = "([A-Fa-f0-9]{2}:[A-Fa-f0-9]{2}:[A-Fa-f0-9]{2})".r

  val behavior: Behavior[Msg] = Behaviors.supervise(
    Behaviors.withTimers((timers: TimerScheduler[Msg]) => {
      timers.startPeriodicTimer(InterfaceRefreshInterval, RefreshNetworkInterfaces, InterfaceRefreshInterval)

      Behaviors.setup((setup: ActorContext[Msg]) => {
        setup.self ! RefreshNetworkInterfaces

        withState(Seq(), Seq(), Map(), timers)
      })
    })
  ).onFailure(SupervisorStrategy.restart)

  private def withState(children: Seq[ActorRef[MdnsInterfaceListener.Message]],
                        listeners: Seq[ActorRef[Map[String, NanoleafAddress]]],
                        lights: Map[String, NanoleafAddress],
                        timers: TimerScheduler[Msg]): Behavior[Msg] = Behaviors.receivePartial[Msg]({
    case (_, ServiceEvent(event)) => {
      val info: ServiceInfo = event.getInfo
      val name: String = info.getName

      val foundLight: Option[NanoleafAddress] = for {
        id <- NameRegex.findFirstIn(name)
        url <- info.getURLs().headOption
      } yield NanoleafAddress(id.toLowerCase, name, url)

      foundLight
        .map((light: NanoleafAddress) => {
          // add our light to the map
          val nextLights: Map[String, NanoleafAddress] = lights + (light.id -> light)

          // tell our listeners about light changes
          listeners.foreach(_ ! nextLights)

          withState(children, listeners, nextLights, timers)
        })
        .getOrElse(Behaviors.same)
    }
    case (_, RegisterListener(listener)) => {
      withState(children, listeners :+ listener, lights, timers)
    }
    case (_, GetLights(ref)) => {
      ref ! lights

      Behaviors.same
    }
    case (ctx, RefreshNetworkInterfaces) => {
      logger.debug("Refreshing network interfaces")

      // shutdown our old children
      children.foreach(ctx.stop(_))

      withState(MdnsInterfaceListener.actors(ctx), listeners, lights, timers)
    }
  }).receiveSignal({
    case (_, PostStop) => {
      timers.cancelAll()

      Behaviors.same
    }
  })
}

