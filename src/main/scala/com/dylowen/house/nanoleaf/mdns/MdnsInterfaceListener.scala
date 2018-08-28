package com.dylowen.house
package nanoleaf.mdns

import java.net.{InetAddress, NetworkInterface}
import java.util

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Terminated}
import com.typesafe.scalalogging.LazyLogging
import javax.jmdns
import javax.jmdns.JmDNS.Delegate
import javax.jmdns.{JmDNS, ServiceInfo, ServiceListener}

import scala.collection.JavaConverters._
import scala.collection.{immutable, mutable}
import scala.util.Random

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
private[mdns] object MdnsInterfaceListener extends LazyLogging {

  import NanoleafMdnsService._

  /**
    * Actor API
    */
  private[mdns] sealed trait Message

  private case class ServiceEvent(event: jmdns.ServiceEvent) extends Message {
    def forParent: NanoleafMdnsService.ServiceEvent = NanoleafMdnsService.ServiceEvent(event)
  }

  private case class MDNSError(errorMessage: String) extends Message


  def actors(context: ActorContext[NanoleafMdnsService.Msg]): Seq[ActorRef[Message]] = {
    getValidInterfaces
      .flatMap((networkInterface: NetworkInterface) => {
        networkInterface.getInetAddresses.asScala
          .map((address: InetAddress) => {
            val friendlyAddress: String = address.getHostAddress
              .replaceAll("%.*$", "")

            context.spawn(
              interfaceActor(address, networkInterface, context.self),
              networkInterface.getDisplayName + "-" + friendlyAddress + "-" + Random.nextInt(Int.MaxValue)
            )
          })
      })
  }

  private def interfaceActor(hostAddress: InetAddress,
                             networkInterface: NetworkInterface,
                             parent: ActorRef[NanoleafMdnsService.Msg]): Behavior[Message] = {
    Behaviors.setup((setup: ActorContext[Message]) => {

      val jmDNS: JmDNS = JmDNS.create(hostAddress)
      val selfListener: SelfListener = new SelfListener(setup.self)

      jmDNS.addServiceListener(NanoleafServiceType, selfListener)
      jmDNS.setDelegate(new SelfDelegate(hostAddress, setup.self))

      logger.debug(s"Registered ${NanoleafMdnsService.NanoleafServiceType} listener on $hostAddress")

      Behaviors.receive[Message]((_: ActorContext[Message], message: Message) => {
        message match {
          case event: ServiceEvent => {
            parent ! event.forParent

            Behaviors.same
          }
          case MDNSError(errorMessage) => {
            logger.warn(errorMessage)

            // shutdown this actor since we received an error
            Behaviors.stopped
          }
        }
      }).receiveSignal({
        case (_, PostStop) => {
          jmDNS.close()

          logger.debug(s"Removed ${NanoleafMdnsService.NanoleafServiceType} listener on $hostAddress")
          Behaviors.same
        }
        case (_, terminated: Terminated) => {
          jmDNS.close()

          logger.error("Exception: ", terminated.failure)

          Behaviors.same
        }
      })
    })
  }

  private class SelfListener(self: ActorRef[Message]) extends ServiceListener {
    override def serviceAdded(event: jmdns.ServiceEvent): Unit = self ! ServiceEvent(event)

    override def serviceRemoved(event: jmdns.ServiceEvent): Unit = self ! ServiceEvent(event)

    override def serviceResolved(event: jmdns.ServiceEvent): Unit = self ! ServiceEvent(event)
  }

  private class SelfDelegate(hostAddress: InetAddress, self: ActorRef[Message]) extends Delegate {
    override def cannotRecoverFromIOError(dns: JmDNS, infos: util.Collection[ServiceInfo]): Unit = {
      // tell our actor we're hitting exception
      self ! MDNSError(s"mDNS IO Errors, stopping interface: $hostAddress")
    }
  }

  private def getValidInterfaces: Seq[NetworkInterface] = {
    val validInterfaces: mutable.Builder[NetworkInterface, immutable.Seq[NetworkInterface]] = immutable.Seq.newBuilder
    val seen: mutable.Set[Array[Byte]] = mutable.Set()

    NetworkInterface.getNetworkInterfaces.asScala
      .filter((interface: NetworkInterface) => {
        // filter out invalid interfaces
        interface.isUp && !interface.isVirtual && interface.supportsMulticast && !interface.isLoopback
      })
      .foreach((interface: NetworkInterface) => {
        // make sure our interfaces all have distinct mac addresses
        val hardwareAddress: Array[Byte] = interface.getHardwareAddress

        if (hardwareAddress == null || !seen.contains(hardwareAddress)) {
          seen += hardwareAddress
          validInterfaces += interface
        }
      })

    validInterfaces.result()
  }
}