package com.dylowen.house

import java.time.{ZoneId, ZonedDateTime}

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.LazyLogging

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object StateToAction extends LazyLogging {

  val initialValue: HouseAction = NoAction

  def apply(): Flow[HouseState, HouseAction, NotUsed] = Flow[HouseState].map((state: HouseState) => {
    logger.info(state.toString)

    val minBrightnessOffset: Int = 5
    val dimHour: Int = 20

    state match {
      // if we don't have any clients and our light is on, turn it off
      case HouseState(Nil, NanoLeafState(_, true), _, _) => LightOff()

      // if we turned our light off and there's a client turn it on
      case HouseState(_ :: _, NanoLeafState(_, false), LightOff(), _) => LightOn()

      // if our light is on check the brightness
      case HouseState(_, NanoLeafState(brightness, true), _, instant) => {
        // our lights are on so check if we should dim them
        val time: ZonedDateTime = instant.atZone(ZoneId.systemDefault())
        val hour: Int = time.getHour
        
        if (hour >= dimHour) {
          val minutes: Int = time.getMinute
          val max: Int = brightness.max - brightness.min - minBrightnessOffset // don't let the light get too dim

          val newBrightness: Int = brightness.max - ((hour - dimHour) * 60 + minutes) * max / ((24 - dimHour) * 60)

          LightBrightness(newBrightness)
        }
        else {
          NoAction
        }
      }
      case _ => NoAction
    }
  })
}