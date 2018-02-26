package com.dylowen.unifi

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import com.dylowen.nanoleaf.NanoSystem
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object ClientWatcher {

  sealed trait ClientEvent

  case class ClientJoined(client: Client) extends ClientEvent

  case class ClientLeft(client: Client) extends ClientEvent

  private val SeenThreshold: FiniteDuration = 10 minutes

  def apply(deviceFilter: (Client) => Boolean,
            unifiAuthorization: UnifiAuthorization,
            interval: FiniteDuration = 1 minute,
            name: String = "")
           (implicit nanoSystem: NanoSystem): Source[ClientEvent, NotUsed] = {
    Source.fromGraph(new ClientWatcher(deviceFilter, unifiAuthorization, interval, name))
  }
}

private class ClientWatcher(val deviceFilter: (Client) => Boolean,
                            val unifiAuthorization: UnifiAuthorization,
                            val interval: FiniteDuration,
                            name: String)
                           (implicit nanoSystem: NanoSystem) extends GraphStage[SourceShape[ClientWatcher.ClientEvent]] with LazyLogging {

  import ClientWatcher._
  import nanoSystem.executionContext

  private final val out: Outlet[ClientEvent] = Outlet[ClientEvent](s"DeviceWatcher($name).out")

  override val shape: SourceShape[ClientEvent] = SourceShape.of(out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {
    private val connectedDevices: mutable.Map[String, Client] = mutable.Map()
    private val eventQueue: mutable.Queue[ClientEvent] = mutable.Queue()
    private var downstreamWaiting: Boolean = false

    private val timerKey: String = "DeviceWatcher"

    override def preStart(): Unit = {
      schedulePeriodicallyWithInitialDelay(timerKey, 100 milliseconds, interval)
    }

    setHandler(out, new OutHandler {

      override def onPull(): Unit = {
        downstreamWaiting = true

        dequeue()
      }
    })

    final override protected def onTimer(key: Any): Unit = {
      val threshold: Instant = Instant.now().minusMillis(SeenThreshold.toMillis)

      val future: Future[Seq[Client]] = GetClients(unifiAuthorization)

      future.failed.map(logger.error("", _))

      future.foreach((devices: Seq[Client]) => {
        val currentDevices: Seq[Client] = devices
          .filter(_.lastSeen.isAfter(threshold))
          .filter(deviceFilter)

        // add new devices
        currentDevices.foreach((device: Client) => {
          val key: String = device.mac
          if (!connectedDevices.contains(key)) {
            val joined: ClientJoined = ClientJoined(device)

            connectedDevices.put(key, device)
            eventQueue.enqueue(joined)
          }
        })

        // remove old devices
        val removedDevices: Iterable[String] = connectedDevices.keys
          .filterNot((key: String) => currentDevices.exists(_.mac == key))

        removedDevices.foreach((deviceKey: String) => {
          val removedDevice: Client = connectedDevices.remove(deviceKey).get

          eventQueue.enqueue(ClientLeft(removedDevice))
        })

        dequeue()
      })
    }

    private def dequeue(): Boolean = {
      if (downstreamWaiting && eventQueue.nonEmpty) {
        val event: ClientEvent = eventQueue.dequeue()
        downstreamWaiting = false

        push(out, event)

        true
      }
      else {
        false
      }
    }
  }
}