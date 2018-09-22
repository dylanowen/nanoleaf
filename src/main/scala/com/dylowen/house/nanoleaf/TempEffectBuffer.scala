package com.dylowen.house.nanoleaf

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import com.dylowen.house.nanoleaf.api.{EffectCommand, NanoleafClient}
import com.dylowen.house.{HouseSystem, Seq}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.{FiniteDuration, _}

/**
  * A buffer for displaying temporary effects, as one right after the other isn't allowed on the device
  *
  * @author dylan.owen
  * @since Sep-2018
  */
private[nanoleaf] object TempEffectBuffer {

  sealed trait Msg

  case object Ready extends Msg

  case class TempEffect(effect: EffectCommand, duration: Int) extends Msg


  // the time between requests to the nanoleaf for setting effects
  private val EffectBufferDuration: FiniteDuration = 300 milliseconds
}

private[nanoleaf] class TempEffectBuffer(implicit override val system: HouseSystem) extends ResponseHandler with LazyLogging {

  import TempEffectBuffer._
  import system.executionContext

  def behavior(client: NanoleafClient): Behavior[Msg] = {
    Behaviors.withTimers((timer: TimerScheduler[Msg]) => {
      waiting(client, timer)
    })
  }

  private def waiting(client: NanoleafClient, timer: TimerScheduler[Msg]): Behavior[Msg] = Behaviors.receiveMessagePartial({
    case effect: TempEffect => {
      // play the next effect
      play(effect, client, timer)

      // set ourselves to waiting to play
      playing(Seq(), client, timer)
    }
    case unexpected => {
      logger.warn(s"Unexpected message: $unexpected")

      waiting(client, timer)
    }
  })

  private def playing(effects: Seq[TempEffect],
                      client: NanoleafClient,
                      timer: TimerScheduler[Msg]): Behavior[Msg] = Behaviors.receiveMessagePartial({
    case effect: TempEffect => {
      playing(effects :+ effect, client, timer)
    }
    case Ready => {
      if (effects.nonEmpty) {
        // play the next effect
        play(effects.head, client, timer)

        // set ourselves to waiting to play
        playing(effects.tail, client, timer)
      }
      else {
        waiting(client, timer)
      }
    }
  })

  private def play(effect: TempEffect, client: NanoleafClient, timer: TimerScheduler[Msg]): Unit = {
    val tempEffect: EffectCommand = effect.effect
      .copy(duration = Some(effect.duration))

    handleResponse(client.tempDisplay(tempEffect), client)
      .andThen({
        case _ => {
          // get our effect duration and add some buffer time
          val waitTime: FiniteDuration = (effect.duration seconds) + EffectBufferDuration

          logger.debug(s"Playing temp effect and waiting ${waitTime.toSeconds} seconds")

          // schedule a timer for when we can play the next effect
          timer.startSingleTimer(Ready, Ready, waitTime)
        }
      })
  }
}