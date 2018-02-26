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
  def apply(): Flow[HouseState, HouseAction, NotUsed] = Flow[HouseState].map((state: HouseState) => {
    logger.info(state.toString)

    val dimHour: Int = 20

    state match {
      case HouseState(_, NanoLeafState(brightness, true, client), _, instant) => {
        // our lights are on so check if we should dim them
        val time: ZonedDateTime = instant.atZone(ZoneId.systemDefault())
        val hour: Int = time.getHour
        if (hour >= dimHour) {
          val minutes: Int = time.getMinute
          val newBrightness: Int = 100 - ((hour - dimHour) * 60 + minutes) * 100 / ((24 - dimHour) * 60)

          LightBrightness(newBrightness, instant, client)
        }
        else {
          NoAction
        }
      }
      case _ => NoAction
    }
  })

  val initialValue: HouseAction = NoAction
}