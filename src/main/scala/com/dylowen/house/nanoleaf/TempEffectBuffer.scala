package com.dylowen.house.nanoleaf

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import com.dylowen.house.nanoleaf.api.{EffectCommand, NanoleafClient}
import com.dylowen.house.{HouseSystem, Seq}
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success}

/**
  * A buffer for displaying temporary effects, as one right after the other isn't allowed on the device
  *
  * @author dylan.owen
  * @since Sep-2018
  */
private[nanoleaf] object TempEffectBuffer {

  // the time between requests to the nanoleaf for setting effects
  private val EffectBufferDuration: FiniteDuration = 300 milliseconds

  sealed trait Msg

  case object Ready extends Msg

  sealed trait PlayMsg extends Msg

  case class Wait(duration: FiniteDuration) extends PlayMsg

  val DefaultWait: Wait = Wait(EffectBufferDuration)

  case class TempEffect(effect: EffectCommand, duration: Int) extends PlayMsg
}

private[nanoleaf] class TempEffectBuffer(client: NanoleafClient)
                                        (implicit override val system: HouseSystem) extends ResponseHandler with LazyLogging {

  import TempEffectBuffer._
  import system.executionContext

  override protected lazy val logger: Logger = Logger(LoggerFactory.getLogger(s"EffectBuffer[${client.address.id}]"))

  def behavior: Behavior[Msg] = {
    Behaviors.withTimers((timer: TimerScheduler[Msg]) => {
      waiting(timer)
    })
  }

  private def waiting(timer: TimerScheduler[Msg]): Behavior[Msg] = Behaviors.receiveMessagePartial({
    case playMessage: PlayMsg => {
      // play the next effect
      play(playMessage, timer)

      // set ourselves to waiting to play
      playing(Seq(), timer)
    }
    case unexpected => {
      logger.warn(s"Unexpected message: $unexpected")

      waiting(timer)
    }
  })

  private def playing(playMessages: Seq[PlayMsg],
                      timer: TimerScheduler[Msg]): Behavior[Msg] = Behaviors.receiveMessagePartial({
    case playMessage: PlayMsg => {
      playing(playMessages :+ playMessage, timer)
    }
    case Ready => {
      if (playMessages.nonEmpty) {
        // play the next effect
        play(playMessages.head, timer)

        // set ourselves to waiting to play
        playing(playMessages.tail, timer)
      }
      else {
        waiting(timer)
      }
    }
  })

  private def play(effect: PlayMsg, timer: TimerScheduler[Msg]): Unit = {
    effect match {
      case effect: TempEffect => {
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
      case Wait(duration) => {
        // schedule a timer for our wait duration
        timer.startSingleTimer(Ready, Ready, duration)
      }
    }
  }
}