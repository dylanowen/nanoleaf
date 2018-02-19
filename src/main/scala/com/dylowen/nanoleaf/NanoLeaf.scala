package com.dylowen.nanoleaf

import java.net.InetAddress

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.dylowen.unifi.{DeviceWatcher, UnifiAuthorization}
import com.typesafe.scalalogging.LazyLogging

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */


object NanoLeaf extends LazyLogging {

  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem("Nanoleaf")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit val system: NanoSystem = NanoSystem(actorSystem, materializer)

    import system.executionContext

    val source = UnifiAuthorization()
      .map(auth => {
        DeviceWatcher((device) => {
          device.mac == "5c:f7:e6:9c:76:b7"
        }, auth)
      })

    source.failed.foreach(logger.error("couldn't auth", _))

    class TestActor() extends Actor {
      override def receive: Receive = {
        case a => {
          println(a)
        }
      }
    }

    val ref = actorSystem.actorOf(Props[TestActor](new TestActor()))

    import net.straylightlabs.hola.dns.Domain
    import net.straylightlabs.hola.sd.{Query, Service}

    val service: Service = Service.fromName("_nanoleafapi._tcp")
    val query: Query = Query.createFor(service, Domain.LOCAL)




    val instances = query.runOnceOn(InetAddress.getByName("192.168.1.15"))
    instances.stream.forEach(System.out.println(_))

    //Dns.resolve("Aurora-51-e6-cd.local")(actorSystem, ref)


    /*

        MDns(searchLoopback = true)
          .map(ref => {
            ref ! Query(Seq("a"), 0, true, 0)
          })
    */
    /*
    val udpPort = actorSystem.actorOf(Props(new StreamingClient(address)))

    Thread.sleep(1000)

    val panels: Array[Int] = Array(
      209, 203, 62, 164, 193, 141, 148, 244, 146, 91, 233, 93, 226, 199, 218
    )

    val panelFrame1: PanelFrame = PanelFrame(100, 0, 100)
    val panelFrame2: PanelFrame = PanelFrame(100, 0, 100)

    val panelEffect209: PanelEffect = PanelEffect(PanelId(209), panelFrame1, panelFrame2)
    val panelEffect203: PanelEffect = PanelEffect(PanelId(203), panelFrame1, panelFrame2)

    val effect: StreamingEffect = StreamingEffect(panelEffect203, panelEffect209)

    println(effect)
    println(effect.getBytes.map(_.toHexString).reduce(_ + " " + _))

    udpPort ! effect.getBytes

    //var i = 0
    //var on = true
    while (true) {

      for (r <- 0 to 255 by 50) {
        for (g <- 0 to 255 by 50) {
          for (b <- 0 to 255 by 50) {

            for (i <- 0 until 15) {
              Thread.sleep(10)
              udpPort ! Array(
                0x01,
                panels(i), 0x01, r, g, b, 0x00, 0x01
              ).map(_.toByte)
            }
          }
        }
      }
      */

    /*

    if (on) {
      udpPort ! Array(
        0x01,
        panels(i), 0x01, 0XF0, 0XF0, 0XF0, 0x00, 0x01
      ).map(_.toByte)
    }
    else {
      udpPort ! Array(
        0x01,
        panels(i), 0x01, 0X00, 0X00, 0X00, 0x00, 0x01
      ).map(_.toByte)
    }


    i += 1
    if (i >= panels.length) {
      on = !on
      i = 0
    }
  }

    udpPort ! Array(
      0x02,
      0xD1, 0x01, 0xFF, 0x00, 0x00, 0x00, 0x01,
      203, 0x01, 0xFF, 0x00, 0x00, 0x00, 0x01
    ).map(_.toByte)



    val opts = List(Inet6ProtocolFamily(), MulticastGroup(group, iface))
    IO(Udp) ! Udp.Bind(self, new InetSocketAddress(port), opts)
    */
  }
}
