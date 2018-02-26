package com.dylowen.house

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.utils._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object ActionHandler extends LazyLogging {

  def apply()(implicit system: NanoSystem): Flow[HouseAction, Unit, NotUsed] = Flow[HouseAction].mapAsync(1)((action: HouseAction) => {
    logger.info(action.toString)

    val interval: FiniteDuration = system.config.getFiniteDuration("nanoleaf.update-interval")

    action match {
      case LightBrightness(brightness, _, client) => client.setBrightness(brightness, Some(interval))
      case _ => Future.unit
    }
  })
}
