package com.dylowen.house

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.nanoleaf.api.NanoLeafClient
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
    if (action != NoAction) {
      logger.info(action.toString)
    }

    val interval: FiniteDuration = system.config.getFiniteDuration("nanoleaf.update-interval")

    action match {
      case LightBrightness(brightness) => actOnClient(_.setBrightness(brightness, Some(interval)))
      case _: LightOn => actOnClient(_.setState(true))
      case _: LightOff => actOnClient(_.setState(false))
      case _ => Future.unit
    }
  })

  private def actOnClient(action: NanoLeafClient => Future[Unit])(implicit system: NanoSystem): Future[Unit] = {
    import system.executionContext

    NanoLeafState.getClient.flatMap(action)
  }
}
