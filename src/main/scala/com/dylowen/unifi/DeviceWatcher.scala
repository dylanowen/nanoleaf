package com.dylowen.unifi

import java.time.Instant

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import com.dylowen.nanoleaf.NanoSystem
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object DeviceWatcher {

  sealed trait DeviceEvent

  case class DeviceJoined(device: Device) extends DeviceEvent

  case class DeviceLeft(device: Device) extends DeviceEvent

  private val SeenThreshold: FiniteDuration = 10 seconds

  def apply(deviceFilter: (Device) => Boolean,
            unifiAuthorization: UnifiAuthorization,
            interval: FiniteDuration = 10 seconds,
            name: String = "")
           (implicit nanoSystem: NanoSystem): Source[DeviceEvent, NotUsed] = {
    Source.fromGraph(new DeviceWatcher(deviceFilter, unifiAuthorization, interval, name))
  }
}

private class DeviceWatcher(val deviceFilter: (Device) => Boolean,
                            val unifiAuthorization: UnifiAuthorization,
                            val interval: FiniteDuration,
                            name: String)
                           (implicit nanoSystem: NanoSystem) extends GraphStage[SourceShape[DeviceWatcher.DeviceEvent]] with LazyLogging {

  import DeviceWatcher._
  import nanoSystem.executionContext

  private final val out: Outlet[DeviceEvent] = Outlet[DeviceEvent](s"DeviceWatcher($name).out")

  override val shape: SourceShape[DeviceEvent] = SourceShape.of(out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {
    private val connectedDevices: mutable.Map[String, Device] = mutable.Map()
    private val eventQueue: mutable.Queue[DeviceEvent] = mutable.Queue()
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

      val future = GetDevices(unifiAuthorization)

      future.failed.map(logger.error("", _))

      future.foreach((devices: Seq[Device]) => {
        val currentDevices: Seq[Device] = devices
          .filter(_.lastSeen.isAfter(threshold))
          .filter(deviceFilter)

        // add new devices
        currentDevices.foreach((device: Device) => {
          val key: String = device.mac
          if (!connectedDevices.contains(key)) {
            val joined: DeviceJoined = DeviceJoined(device)

            connectedDevices.put(key, device)
            eventQueue.enqueue(joined)
          }
        })

        // remove old devices
        val removedDevices: Iterable[String] = connectedDevices.keys
          .filterNot((key: String) => currentDevices.exists(_.mac == key))

        removedDevices.foreach((deviceKey: String) => {
          val removedDevice: Device = connectedDevices.remove(deviceKey).get

          eventQueue.enqueue(DeviceLeft(removedDevice))
        })

        dequeue()
      })
    }

    private def dequeue(): Boolean = {
      if (downstreamWaiting && eventQueue.nonEmpty) {
        val event: DeviceEvent = eventQueue.dequeue()
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