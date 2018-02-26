package com.dylowen.house

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.nanoleaf.api.{NanoLeafClient, NanoLeafHouse}

import scala.concurrent.Future

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object NanoLeafState {
  def apply()(implicit nanoSystem: NanoSystem): Flow[Any, NanoLeafState, NotUsed] = Flow[Any].mapAsync(1)((_: Any) => {
    import nanoSystem.executionContext

    val house: NanoLeafHouse = new NanoLeafHouse()

    house.getClients
      .flatMap((client: NanoLeafClient) => {
        val onFuture: Future[Boolean] = client.isOn
        val brightnessFuture: Future[Int] = client.brightness

        for {
          on <- onFuture
          brightness <- brightnessFuture
        } yield NanoLeafState(brightness, on, client)
      })
  })
}

case class NanoLeafState(brightness: Int, on: Boolean, client: NanoLeafClient)
