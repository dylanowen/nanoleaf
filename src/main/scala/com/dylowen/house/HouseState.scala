package com.dylowen.house

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, ZipWith3}
import akka.stream.{FanInShape2, _}
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.unifi.Client

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object HouseState {
  def apply()(implicit system: NanoSystem): Flow[HouseAction, HouseState, NotUsed] = {
    Flow.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => {
      import GraphDSL.Implicits._

      val tickBroadcast: UniformFanOutShape[HouseAction, HouseAction] = builder.add(Broadcast(3))

      val merger: FanInShape3[Seq[Client], NanoLeafState, HouseAction, HouseState] = builder.add(new ZipWith3(HouseState.apply(_, _, _)))

      val clientStateFlow: FlowShape[Any, Seq[Client]] = builder.add(ClientState())
      val nanoLeafStateFlow: FlowShape[Any, NanoLeafState] = builder.add(NanoLeafState())

      tickBroadcast.out(0) ~> clientStateFlow ~> merger.in0
      tickBroadcast.out(1) ~> nanoLeafStateFlow ~> merger.in1
      tickBroadcast.out(2) ~> merger.in2

      new FlowShape[HouseAction, HouseState](tickBroadcast.in, merger.out)
    }})
  }
}

case class HouseState(clients: Seq[Client], nanoLeafState: NanoLeafState, lastAction: HouseAction, time: Instant = Instant.now)
