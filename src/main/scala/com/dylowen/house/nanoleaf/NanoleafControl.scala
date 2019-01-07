package com.dylowen.house
package nanoleaf

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId, ZonedDateTime}

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.dylowen.house.control.{ClientConfig, HouseState}
import com.dylowen.house.nanoleaf.api._
import com.dylowen.house.nanoleaf.mdns.NanoleafAddress
import com.dylowen.house.unifi.WifiClient
import com.dylowen.house.utils._
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.slf4j.LoggerFactory

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

  private case class ReceivedLightState(lightState: NanoleafState) extends Msg

  private case object MissingLightState extends Msg

  // how long we play a new phone effect
  private val NewPhoneDuration: Int = 20

  object NonEmpty {
    def unapply[T](seq: Seq[T]): Option[Seq[T]] = {
      if (seq.nonEmpty) {
        Some(seq)
      }
      else {
        None
      }
    }
  }

  private object InternalState {
    val Initial: InternalState = InternalState(
      manualModeInstant = None,
      action = NoAction,
      lightState = NanoleafState(
        Brightness(0, 0, 0),
        on = false,
        effect = ""
      )
    )
  }

  private case class InternalState(manualModeInstant: Option[Instant],
                                   action: NanoleafAction,
                                   lightState: NanoleafState) {
    def manualMode: Boolean = {
      manualModeInstant
        .exists(_.plus(6, ChronoUnit.HOURS).isAfter(Instant.now()))
    }
  }

  private val NoStateChange: NextStateChanges = NextStateChanges()

  private case class NextStateChanges(manualMode: Option[Boolean] = None,
                                      action: Option[NanoleafAction] = None,
                                      lightStateOverride: Option[NanoleafState] = None,
                                      tempEffects: Seq[TempEffectBuffer.PlayMsg] = Seq()) {

    private type Updater = InternalState => InternalState

    def next(internalState: InternalState, lightState: NanoleafState): InternalState = {
      val stateUpdaters: Seq[Updater] = Seq(actionUpdater, manualModeUpdater, lightStateUpdater)
        .collect({
          case Some(updater) => updater
        })

      // take our light state initially, so we can override it if needed
      val updatedState: InternalState = internalState.copy(lightState = lightState)

      stateUpdaters
        .foldLeft(updatedState)((result: InternalState, updater: Updater) => {
          updater(result)
        })
    }

    def actionUpdater: Option[Updater] = {
      action.map((next: NanoleafAction) => _.copy(action = next))
    }

    def manualModeUpdater: Option[Updater] = {
      manualMode.map((next: Boolean) => {
        _.copy(manualModeInstant = if (next) Some(Instant.now()) else None)
      })
    }

    def lightStateUpdater: Option[Updater] = {
      lightStateOverride.map((next: NanoleafState) => _.copy(lightState = next))
    }
  }

}

class NanoleafControl(address: NanoleafAddress,
                      private val config: NanoleafConfig)
                     (implicit override val system: HouseSystem) extends ResponseHandler with LazyLogging {

  import NanoleafControl._
  import system.executionContext

  override protected lazy val logger: Logger = Logger(LoggerFactory.getLogger(s"${getClass.getName}[${address.id}]"))

  private val UpdateInterval: FiniteDuration = system.config.getFiniteDuration("nanoleaf.update-interval")

  private val myPhonesEffects: Map[String, EffectCommand] = ClientConfig.configs(system.config.getConfig("clients.phones"))
    .map((entry: (String, ClientConfig)) => {
      entry._1 -> DefinedEffect(entry._2.effect)
    })
  private val unknownClientEffect: EffectCommand = DefinedEffect(ClientConfig(system.config.getConfig("clients.unknown-client")).effect)

  private val client: NanoleafClient = NanoleafClient(address, config.auth)

  def behavior: Behavior[Msg] = {
    Behaviors.setup((setup: ActorContext[Msg]) => {

      val tempEffectBuffer: TempEffectBuffer = new TempEffectBuffer(client)

      val buffer: ActorRef[TempEffectBuffer.Msg] = setup
        .spawn(tempEffectBuffer.behavior, s"temp-effect-buffer-${address.id}")

      // effect testing
      //buffer ! TempEffectBuffer.TempEffect(DefinedEffect.Default, 10)

      waiting(
        InternalState.Initial,
        buffer,
      )
    })
  }

  private def waiting(internalState: InternalState,
                      buffer: ActorRef[TempEffectBuffer.Msg]): Behavior[Msg] = {
    Behaviors.receivePartial[Msg]({
      case (ctx, Tick(houseState)) => {
        // request our light state and move to waiting for a response
        getLightState(client)
          .foreach((response: Option[NanoleafState]) => {
            response match {
              case Some(lightState) => {
                ctx.self ! ReceivedLightState(lightState)
              }
              case None => {
                ctx.self ! MissingLightState
              }
            }
          })

        gettingState(houseState, internalState, buffer)
      }
      case unexpected => {
        logger.warn(s"received $unexpected while waiting")

        Behaviors.same
      }
    })
  }

  private def gettingState(houseState: HouseState,
                           internalState: InternalState,
                           buffer: ActorRef[TempEffectBuffer.Msg]): Behavior[Msg] = {
    Behaviors.receiveMessagePartial[Msg]({
      case ReceivedLightState(lightState) => {

        // log our light state
        logger.debug(s"state: $lightState")
        logger.debug(s"internal-state" +
          s" manualMode: ${internalState.manualMode}" +
          s" action: ${internalState.action.getClass.getSimpleName}")

        val stateDelta: NextStateChanges = getNextStateActions(lightState, houseState, internalState)

        // if we have a non-NoAction action run it
        stateDelta.action.foreach((action: NanoleafAction) => {
          // log what action we're running
          logger.info(s"Running action: $action")

          actOnClient(action, client, buffer)
        })

        // if we have any temp effects run them
        stateDelta.tempEffects.foreach(buffer ! _)

        // update with our delta changes and the last light state
        val nextState: InternalState = stateDelta.next(internalState, lightState)

        // set our next action if we have one or use our last one
        waiting(
          internalState = nextState,
          buffer = buffer,
        )
      }
      case MissingLightState => {
        // the state wasn't updated so don't do anything
        waiting(
          internalState = internalState,
          buffer = buffer,
        )
      }
      case unexpected => {
        logger.warn(s"received $unexpected while getting state")

        Behaviors.same
      }
    })
  }

  private def getLightState(client: NanoleafClient): Future[Option[NanoleafState]] = {
    val brightnessRequest: Future[Option[Brightness]] = handleResponse(client.brightness, client)
    val isOnRequest: Future[Option[Boolean]] = handleResponse(client.isOn, client)
    val effectRequest: Future[Option[String]] = handleResponse(client.effect, client)

    for {
      maybeBrightness <- brightnessRequest
      maybeIsOn <- isOnRequest
      maybeEffect <- effectRequest
    } yield {
      for {
        brightness <- maybeBrightness
        isOn <- maybeIsOn
        effect <- maybeEffect
      } yield NanoleafState(brightness, isOn, effect)
    }
  }

  private def getNextStateActions(lightState: NanoleafState,
                                  houseState: HouseState,
                                  internalState: InternalState): NextStateChanges = {
    (houseState, lightState, internalState) match {
      // if we don't have any phones and our light is on, turn it off
      case (HouseState(Seq(), _, _, _), NanoleafState(_, true, _), _) => {
        NextStateChanges(
          action = Some(LightOff)
        )
      }

      // if we turned our light off and there's a phone turn it on
      case (HouseState(NonEmpty(phones), _, _, _), NanoleafState(_, false, _), InternalState(_, LightOff, _)) => {
        // turn the lights on, then wait and run our phone announcements
        NextStateChanges(
          action = Some(LightOn),
          tempEffects = TempEffectBuffer.DefaultWait +: knownPhoneEffects(phones)
        )
      }

      // if our light is on
      case (_, NanoleafState(brightness, true, _), _) => {
        if (houseState.newPhones.nonEmpty) {
          // check if someone just came home
          NextStateChanges(
            tempEffects = knownPhoneEffects(houseState.newPhones)
          )
        }
        else if (houseState.newWirelessClients.nonEmpty) {
          // check if a random wifi device just joined the network
          NextStateChanges(
            tempEffects = Seq(TempEffectBuffer.TempEffect(unknownClientEffect, NewPhoneDuration))
          )
        }
        else if (config.manualMode.contains(lightState.effect)) {
          val nextManualMode: Boolean = !internalState.manualMode

          logger.debug(s"Setting Manual Mode: $nextManualMode")

          val confirmManualMode: EffectCommand = if (nextManualMode) {
            DefinedEffect.ManualModeOn
          }
          else {
            DefinedEffect.ManualModeOff
          }

          val lastEffect: String = internalState.lightState.effect

          NextStateChanges(
            action = Some(LightEffect(lastEffect)),
            manualMode = Some(nextManualMode),
            lightStateOverride = Some(lightState.copy(effect = lastEffect)),
            tempEffects = Seq(
              TempEffectBuffer.DefaultWait,
              TempEffectBuffer.TempEffect(confirmManualMode, 2)
            )
          )
        }
        else {
          // check the brightness
          val dimHour: Int = config.dimHour
          val now: Instant = Instant.now

          // our lights are on so check if we should dim them
          val time: ZonedDateTime = now.atZone(ZoneId.systemDefault())
          val hour: Int = time.getHour

          if (hour >= dimHour) {
            // we should be dimming our lights but see if we're in manual mode
            if (!internalState.manualMode) {
              val minutes: Int = time.getMinute
              val max: Int = brightness.max - brightness.min - config.minBrightness // don't let the light get too dim

              val newBrightness: Int = brightness.max - ((hour - dimHour) * 60 + minutes) * max / ((24 - dimHour) * 60)

              NextStateChanges(action = Some(LightBrightness(newBrightness)))
            }
            else {
              // we're in manual mode so don't do anything
              NoStateChange
            }
          }
          else {
            NoStateChange
          }
        }
      }
      case _ => NoStateChange
    }
  }

  private def actOnClient(action: NanoleafAction, client: NanoleafClient, buffer: ActorRef[TempEffectBuffer.Msg]): Unit = {
    val response: Future[Any] = action match {

      case LightBrightness(brightness) => {
        handleResponse(client.setBrightness(brightness, Some(UpdateInterval)), client)
      }

      case LightOn => {
        handleResponse(client.setState(true), client)
      }

      case LightOff => {
        handleResponse(client.setState(false), client)
      }

      case LightEffect(effect) => {
        handleResponse(client.selectEffect(effect), client)
      }

      case NoAction => Future.unit
    }

    response.onComplete({
      case _: Success[_] => // noop
      case Failure(exception) => {
        logger.error(s"error updating lights", exception)
      }
    })
  }

  private def knownPhoneEffects(phones: Seq[WifiClient]): Seq[TempEffectBuffer.PlayMsg] = {
    // get the duration for our effects
    val clientDuration: Int = NewPhoneDuration / phones.length

    // find an effect for the phones or get a default one
    phones
      .map(_.mac)
      .map(myPhonesEffects.getOrElse(_, DefinedEffect.Default))
      .map(TempEffectBuffer.TempEffect(_, clientDuration))
  }
}