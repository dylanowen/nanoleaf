package com.dylowen.mdns

import java.net._
import java.nio.channels.DatagramChannel

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.io.Inet.{DatagramChannelCreator, SocketOptionV2}
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.utils.ActorStartup
import com.dylowen.utils.Binary._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.collection.{immutable, mutable}
import scala.concurrent.Future

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object MDns {

  // sealed trait MDnsQuery

  //case object Query extends MDnsQuery

  private sealed trait AddressType {
    def protocolFamily: ProtocolFamily

    val port: Int = 5353

    def multicastGroup: InetAddress
  }

  private case object Ipv4 extends AddressType {
    override def protocolFamily: ProtocolFamily = StandardProtocolFamily.INET

    override val multicastGroup: InetAddress = InetAddress.getByName("224.0.0.251")
  }

  private case object Ipv6 extends AddressType {
    override def protocolFamily: ProtocolFamily = StandardProtocolFamily.INET6

    override val multicastGroup: InetAddress = InetAddress.getByName("FF02::FB")
  }

  private final case class MulticastCreator(protocolFamily: ProtocolFamily, interface: NetworkInterface) extends DatagramChannelCreator {
    override def create(): DatagramChannel = {
      val channel: DatagramChannel = DatagramChannel.open(protocolFamily)
      channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, interface)

      channel
    }
  }

  private final case class MulticastGroup(group: InetAddress, interface: NetworkInterface) extends SocketOptionV2 {
    override def afterBind(s: DatagramSocket) {
      s.getChannel.join(group, interface)
    }
  }

  def apply(searchLoopback: Boolean = true)
           (implicit system: NanoSystem): Future[ActorRef] = {
    import system.actorSystem

    val actor: ActorRef = actorSystem.actorOf(Props(new MDns(searchLoopback)), name = "mDNS")

    ActorStartup(actor)
  }
}

class MDns private(searchLoopback: Boolean = false) extends Actor with ActorStartup with LazyLogging {

  import MDns._

  val childActors: mutable.Map[ActorRef, Boolean] = mutable.Map(getValidInterfaces(searchLoopback)
    .flatMap((interface: NetworkInterface) => {
      getInterfaceAddressTypes(interface)
        .map(openSocket(self, interface, _))
    })
    .map(_ -> false): _*)

  def readySockets: immutable.Iterable[ActorRef] = {
    childActors
      .filter(_._2)
      .keys.to[immutable.Iterable]
  }

  override def receive: Receive = preStartup({
    case MDnsSocket.Ready => {
      childActors.update(sender(), true)

      //logger.trace("Actor socket ready: " + sender())
      if (childActors.values.forall((ready: Boolean) => ready)) {
        context.become(ready)

        triggerStartup()
      }
    }
  })

  def ready: Receive = postStartup({
    case query: Query => {
      println("sending query", query)

      val data: Array[Byte] = Array(
        0x00, 0x01, // Transaction Id
        0x00, 0x00, // Flags
        0x00, 0x01, // Number of questions
        0x00, 0x00, // Number of answers
        0x00, 0x00, // Number of authority resource records
        0x00, 0x00, // Number of additional resource records
        //0x0f, // length
        //0x41, 0x75, 0x72, 0x6f, 0x72, 0x61, 0x20, 0x35, 0x31, 0x3a, 0x65, 0x36, 0x3a, 0x63, 0x64, // Aurora 51:e6:cd
        0x0c, // length
        0x5f, 0x6e, 0x61, 0x6e, 0x6f, 0x6c, 0x65, 0x61, 0x66, 0x61, 0x70, 0x69, // _nanoleafapi
        0x04, // length
        0x5f, 0x74, 0x63, 0x70, // _tcp
        0x05, // length
        0x6c, 0x6f, 0x63, 0x61, 0x6c, // local
        0x00, // terminator
        //0x00, 0x21, // record type SRV
        0x00, 0x0c, // record type PTR
        //0x00, 0x01, // record type A
        0x80, 0x01 // record type
      )

      readySockets.foreach(_ ! data)
    }
  })

  private def openSocket(controller: ActorRef, interface: NetworkInterface, addressType: AddressType)
                        (implicit context: ActorContext): ActorRef = {
    logger.trace(s"Registering $addressType multicast group $interface ${addressType.multicastGroup}")

    context.actorOf(Props(new MDnsSocket(controller,
      new InetSocketAddress(addressType.multicastGroup, addressType.port),
      MulticastCreator(addressType.protocolFamily, interface),
      MulticastGroup(addressType.multicastGroup, interface)
    )), name = interface.getDisplayName + addressType)
  }

  private def getValidInterfaces(searchLoopback: Boolean): Seq[NetworkInterface] = {
    val validInterfaces: mutable.Builder[NetworkInterface, immutable.Seq[NetworkInterface]] = immutable.Seq.newBuilder
    val seen: mutable.Set[Array[Byte]] = mutable.Set()

    NetworkInterface.getNetworkInterfaces.asScala
      .filter((interface: NetworkInterface) => {
        // filter out invalid interfaces
        interface.isUp && !interface.isVirtual && interface.supportsMulticast && (searchLoopback || !interface.isLoopback)
      })
      .foreach((interface: NetworkInterface) => {
        // make sure our interfaces all have distinct mac addresses
        val hardwareAddress: Array[Byte] = interface.getHardwareAddress

        if (hardwareAddress == null || !seen.contains(hardwareAddress)) {
          seen += hardwareAddress
          validInterfaces += interface
        }
      })

    val result: Seq[NetworkInterface] = validInterfaces.result()
    logger.trace(s"Found valid interfaces: " + {
      result.map((interface: NetworkInterface) => {
        interface.getDisplayName + getInterfaceAddressTypes(interface)
          .map(_.getClass.getSimpleName)
          .mkString("(", ",", ")")
      }).mkString(", ")
    })

    result
  }

  private def getInterfaceAddressTypes(interface: NetworkInterface): Set[AddressType] = {
    interface.getInetAddresses.asScala
      .collect({
        case _: Inet4Address => Ipv4
        case _: Inet6Address => Ipv6
      })
      .toSet
  }
}
