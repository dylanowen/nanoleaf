package com.dylowen.house
package nanoleaf

import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.dylowen.house.control.{ClientConfig, HouseState}
import com.dylowen.house.nanoleaf.api._
import com.dylowen.house.nanoleaf.mdns.NanoleafAddress
import com.dylowen.house.utils.{ClientError, _}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object NanoleafControl {

  /**
    * Actor API
    */
  trait Msg

  case class Tick(state: HouseState) extends Msg

  private case class ReceivedLightState(maybeLightState: Option[NanoleafState]) extends Msg

  private val NewClientDuration: Int = 15
}

class NanoleafControl(implicit system: HouseSystem) extends LazyLogging {

  import NanoleafControl._
  import system.executionContext

  private val UpdateInterval: FiniteDuration = system.config.getFiniteDuration("nanoleaf.update-interval")

  private val myPhonesEffects: Map[String, EffectCommand] = ClientConfig.configs(system.config.getConfig("clients.phones"))
    .map((entry: (String, ClientConfig)) => {
      entry._1 -> DefinedEffect(entry._2.effect)
          .copy(duration = Some(NewClientDuration))
    })
  private val unknownClientEffect: EffectCommand = DefinedEffect(ClientConfig(system.config.getConfig("clients.unknown-client")).effect)
    .copy(duration = Some(NewClientDuration))

  def behavior(address: NanoleafAddress): Behavior[Msg] = {
    val config: NanoleafConfig = NanoleafConfig(system.config.getConfig(s"""nanoleaf.devices."${address.id}""""))

    val client: NanoleafClient = NanoleafClient(address, config.auth)

    waiting(NoAction, client, config)
  }

  private def waiting(lastAction: NanoleafAction,
                      client: NanoleafClient,
                      config: NanoleafConfig): Behavior[Msg] = {
    Behaviors.receivePartial[Msg]({
      case (ctx, Tick(houseState)) => {
        // request our light state and move to waiting for a response
        getLightState(client)
          .foreach((response: Option[NanoleafState]) => {
            ctx.self ! ReceivedLightState(response)
          })

        gettingState(houseState, lastAction, client, config)
      }
      case unexpected => {
        logger.warn(s"[${client.address}] received $unexpected while waiting")

        Behaviors.same
      }
    })
  }

  private def gettingState(houseState: HouseState,
                           lastAction: NanoleafAction,
                           client: NanoleafClient,
                           config: NanoleafConfig): Behavior[Msg] = {
    Behaviors.receiveMessagePartial[Msg]({
      case ReceivedLightState(maybeState) => {
        // if we have a state find an non-NoAction action
        val nextAction: Option[NanoleafAction] = maybeState
          .map((lightState: NanoleafState) => {
            // log our light state
            logger.debug(s"[${client.address}] state: $lightState last-action: $lastAction")

            getAction(lightState, houseState, lastAction, config)
          })
          .filter(_ != NoAction)

        // if we have a non-NoAction action run it
        nextAction.foreach((action: NanoleafAction) => {
          // log what action we're running
          logger.info(s"[${client.address}] Running action: $action")

          actOnClient(action, client)
        })

        // set our next action if we have one or use our last one
        waiting(nextAction.getOrElse(lastAction), client, config)
      }
      case unexpected => {
        logger.warn(s"[${client.address}] received $unexpected while getting state")

        Behaviors.same
      }
    })
  }

  private def getLightState(client: NanoleafClient): Future[Option[NanoleafState]] = {
    val brightnessRequest: Future[Option[Brightness]] = clientRequest(_.brightness, client)
    val isOnRequest: Future[Option[Boolean]] = clientRequest(_.isOn, client)

    for {
      maybeBrightness <- brightnessRequest
      maybeIsOn <- isOnRequest
    } yield {
      for {
        brightness <- maybeBrightness
        isOn <- maybeIsOn
      } yield NanoleafState(brightness, isOn)
    }
  }

  private def getAction(lightState: NanoleafState,
                        houseState: HouseState,
                        lastAction: NanoleafAction,
                        config: NanoleafConfig): NanoleafAction = {
    (houseState, lightState, lastAction) match {
      // if we don't have any phones and our light is on, turn it off
      case (HouseState(Seq(), _, _, _), NanoleafState(_, true), _) => LightOff

      // if we turned our light off and there's a phone turn it on
      case (HouseState(Seq(_, _*), _, _, _), NanoleafState(_, false), LightOff) => LightOn

      // if our light is on
      case (_, NanoleafState(brightness, true), _) => {
        if (houseState.newPhones.nonEmpty) {
          // check if someone just came home
          NotifyNewPhone(houseState.newPhones)
        }
        else if (houseState.newWirelessClients.nonEmpty) {
          // check if a random wifi device just joined the network
          NotifyNewClients(houseState.newWirelessClients)
        }
        else {
          // check the brightness
          val dimHour: Int = config.dimHour
          val now: Instant = Instant.now

          // our lights are on so check if we should dim them
          val time: ZonedDateTime = now.atZone(ZoneId.systemDefault())
          val hour: Int = time.getHour

          if (hour >= dimHour) {
            val minutes: Int = time.getMinute
            val max: Int = brightness.max - brightness.min - config.minBrightness // don't let the light get too dim

            val newBrightness: Int = brightness.max - ((hour - dimHour) * 60 + minutes) * max / ((24 - dimHour) * 60)

            LightBrightness(newBrightness)
          }
          else {
            NoAction
          }
        }
      }
      case _ => NoAction
    }
  }

  private def actOnClient(action: NanoleafAction, client: NanoleafClient): Unit = {
    val response: Future[Any] = action match {
      case LightBrightness(brightness) => clientRequest(_.setBrightness(brightness, Some(UpdateInterval)), client)
      case LightOn => clientRequest(_.setState(true), client)
      case LightOff => clientRequest(_.setState(false), client)
      case NotifyNewPhone(phones) => {
        // find an effect for this phone or get a default one
        val effect: EffectCommand = phones.headOption
            .map(_.mac)
            .map(myPhonesEffects)
            .getOrElse(DefinedEffect.Default)

        clientRequest(_.tempDisplay(effect), client)
      }
      case NotifyNewClients(_) => {
        clientRequest(_.tempDisplay(unknownClientEffect), client)
      }
      case _ => Future.unit
    }

    response.onComplete({
      case _: Success[_] => // noop
      case Failure(exception) => {
        logger.error(s"[${client.address}] error updating lights", exception)
      }
    })
  }

  private def clientRequest[T](request: NanoleafClient => Future[Either[ClientError, T]],
                               client: NanoleafClient): Future[Option[T]] = {
    request(client)
      .map({
        case Right(t) => Some(t)
        case Left(error) => {
          error.logError(s"[${client.address}] client error", logger)

          None
        }
      })
  }
}