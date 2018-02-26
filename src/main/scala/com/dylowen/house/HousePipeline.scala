package com.dylowen.house

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith2}
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.utils._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object HousePipeline {
  def apply()(implicit system: NanoSystem): RunnableGraph[Cancellable] = {
    import system.{executionContext, materializer}

    val interval: FiniteDuration = system.config.getFiniteDuration("nanoleaf.update-interval")

    // our main flow that can't be trusted to always run successfully
    val mainFlow: Flow[HouseAction, HouseAction, NotUsed] = Flow.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] => {
        import GraphDSL.Implicits._

        val houseAction: FlowShape[HouseAction, HouseAction] = builder.add(HouseState()
          .via(StateToAction()))

        val splitter: UniformFanOutShape[HouseAction, HouseAction] = builder.add(Broadcast(2, eagerCancel = true))

        val actionHandler: FlowShape[HouseAction, Unit] = builder.add(ActionHandler())

        val joiner: FanInShape2[Unit, HouseAction, HouseAction] = builder.add(new ZipWith2((_: Unit, action: HouseAction) => action))

        houseAction ~> splitter ~> actionHandler ~> joiner.in0
        splitter.out(1) ~> joiner.in1

        FlowShape(houseAction.in, joiner.out)
      }
    })

    val loopingFlow: Flow[Unit, Unit, NotUsed] = Flow.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] => {
        import GraphDSL.Implicits._

        val joiner: FanInShape2[Unit, HouseAction, HouseAction] = builder.add(new ZipWith2((_: Unit, action: HouseAction) => action))

        // bundle our house pipeline up in a map async so we can retry the entire graph if it fails
        val flowWrapper: FlowShape[HouseAction, HouseAction] = builder.add(Flow[HouseAction]
          .mapAsync(1)((lastAction: HouseAction) => {
            Source.single(lastAction)
              .via(mainFlow)
              .log("House pipeline failure")
              .runWith(Sink.head)
              .recover({
                case NonFatal(_) => {
                  // push our last action in if we failed
                  lastAction
                }
              })
          }))

        val splitter: UniformFanOutShape[HouseAction, HouseAction] = builder.add(Broadcast(2, eagerCancel = true))

        val dropper: FlowShape[HouseAction, Unit] = builder.add(Flow[HouseAction].map((_: HouseAction) => (): Unit))

        // prestart our HouseAction with an initial value
        val concat: UniformFanInShape[HouseAction, HouseAction] = builder.add(Concat())
        val start: SourceShape[HouseAction] = builder.add(Source.single(StateToAction.initialValue))

        joiner.out ~> flowWrapper ~> splitter ~> dropper

        concat.in(0) <~ start
        concat.in(1) <~ splitter.out(1)
        joiner.in1 <~ concat.out

        FlowShape(joiner.in0, dropper.out)
      }
    })

    Source.tick(0 seconds, interval, (): Unit)
      .via(loopingFlow)
      .withAttributes(supervisionStrategy(Supervision.restartingDecider))
      .log("Interval pipeline failure")
      .to(Sink.ignore)
  }
}
