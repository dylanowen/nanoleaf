package com.dylowen.nanoleaf.mdns

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, PostStop, SupervisorStrategy}
import com.typesafe.scalalogging.LazyLogging
import javax.jmdns
import javax.jmdns.ServiceInfo

import scala.collection.immutable
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.matching.Regex

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object NanoleafService extends LazyLogging {

  /**
    * Actor API
    */
  sealed trait Message

  private[mdns] case class ServiceEvent(event: jmdns.ServiceEvent) extends Message

  private case object RefreshNetworkInterfaces extends Message

  case class GetLights(ref: ActorRef[Lights]) extends Message

  case class Lights(lights: immutable.Seq[NanoleafLight])

  case class NanoleafLight(id: String, name: String, url: String)

  private val InterfaceRefreshInterval: FiniteDuration = 5 seconds

  private[nanoleaf] val NanoleafServiceType: String = "_nanoleafapi._tcp.local."
  private val NameRegex: Regex = "([abcdef0-9]{2}:[abcdef0-9]{2}:[abcdef0-9]{2})".r

  val behavior: Behavior[Message] = Behaviors.supervise(
    Behaviors.withTimers((timers: TimerScheduler[Message]) => {
      timers.startPeriodicTimer(InterfaceRefreshInterval, RefreshNetworkInterfaces, InterfaceRefreshInterval)

      Behaviors.setup((setup: ActorContext[Message]) => {
        setup.self ! RefreshNetworkInterfaces

        withState(Seq(), Map(), timers)
      })
    })
  ).onFailure(SupervisorStrategy.restart)

  private def withState(children: Seq[ActorRef[InterfaceListener.Message]],
                        lights: immutable.Map[String, NanoleafLight],
                        timers: TimerScheduler[Message]): Behavior[Message] = Behaviors.receive[Message](
    (ctx: ActorContext[Message], message: Message) => message match {
      case ServiceEvent(event) => {
        val info: ServiceInfo = event.getInfo
        val name: String = info.getName

        val foundLight: Option[NanoleafLight] = for {
          id <- NameRegex.findFirstIn(name)
          url <- info.getURLs().headOption
        } yield NanoleafLight(id, name, url)

        foundLight
          .map((light: NanoleafLight) => {
            // add our light to the map
            withState(children, lights + (light.id -> light), timers)
          })
          .getOrElse(Behaviors.same)
      }
      case GetLights(ref) => {
        ref ! Lights(lights.values.to[immutable.Seq])

        Behaviors.same
      }
      case RefreshNetworkInterfaces => {
        logger.debug("Refreshing network interfaces")

        // shutdown our old children
        children.foreach(ctx.stop(_))

        withState(InterfaceListener.actors(ctx), lights, timers)
      }
    }).receiveSignal({
    case (_, PostStop) => {
      timers.cancelAll()

      Behaviors.same
    }
  })
}

