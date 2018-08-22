package com.dylowen.nanoleaf

import akka.actor.typed.ActorRef
import akka.stream.ActorMaterializer
import akka.{actor => untyped}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import akka.actor.typed.scaladsl.adapter._
import com.dylowen.unifi.{ClientActor, GetClients, UnifiClientError, WifiClient}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try


/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */


object NanoLeaf extends LazyLogging {

  def main(args: Array[String]): Unit = {
    //implicit val actorSystem: ActorSystem[NanoleafService.Message] = ActorSystem(NanoleafService.behavior, "service")

    implicit val untypedSystem: untyped.ActorSystem = untyped.ActorSystem("HouseManager")
    implicit val materializer: ActorMaterializer = ActorMaterializer()


    implicit val system: NanoSystem = NanoSystem(untypedSystem, materializer)

    val test = untypedSystem.spawn(new ClientActor().behavior, "client")

    implicit val timeout: Timeout = 30 seconds
    implicit val scheduler = untypedSystem.scheduler

    val response = test ? ((ref: ActorRef[Try[Either[UnifiClientError, Seq[WifiClient]]]]) => ClientActor.UnifiRequest(GetClients(_), ref))

    import system.executionContext

    response.foreach(println)

    // construct the pipeline for running our house and start it
    //HousePipeline().run()

    /*
    implicit val timeout: Timeout = 3 seconds
    implicit val scheduler = actorSystem.scheduler

    import actorSystem.executionContext

    while (true) {
      val result: Future[Lights] = actorSystem ? NanoleafService.GetLights

      result.foreach(println)

      Thread.sleep(5000)
    }
    */

    //actorSystem ! NanoleafService.RefreshNetworkInterfaces


    //implicit val actorSystem: ActorSystem = ActorSystem("Nanoleaf")
    //implicit val materializer: ActorMaterializer = ActorMaterializer()

    //implicit val system: NanoSystem = NanoSystem(actorSystem, materializer)

    //val localHost = InetAddress.getLocalHost

    //actorSystem.actorOf(NanoleafServiceActor.props)

    /*
        import materializer.executionContext


        import net.straylightlabs.hola.dns.Domain
        val lookup = new Nothing(MulticastDNSService.DEFAULT_REGISTRATION_DOMAIN_NAME, MulticastDNSService.REGISTRATION_DOMAIN_NAME, MulticastDNSService.DEFAULT_BROWSE_DOMAIN_NAME, MulticastDNSService.BROWSE_DOMAIN_NAME, MulticastDNSService.LEGACY_BROWSE_DOMAIN_NAME)

        val domains: Array[Domain] = lookup.lookupDomains
        for (domain <- domains) {
          System.out.println(domain)
        }

        val mdns: MDnsService = new MDnsService("_nanoleafapi._tcp")


        mdns.query
          .foreach((instances: Set[Instance]) => {
            println(instances)
            instances.foreach((instance: Instance) => {
              instance.lookupAttribute("id")
            })
          })




        implicit val backend: SttpBackend[Future, Nothing] = AkkaHttpBackend()


        sttp.get(uri"http://bdf.arcwb.com/")
          .send()
          .foreach((response: Response[String]) => {
            response.body match {
              case Right(body) => println(body)
              case Left(errorMessage) => println(errorMessage)
            }
          })






        // TODO I should use ping https://stackoverflow.com/questions/11506321/how-to-ping-an-ip-address

        / *
        val source: Future[Source[ClientEvent, NotUsed]] = UnifiAuthorization()
          .map((auth: UnifiAuthorization) => {
            ClientWatcher(_.mac == "5c:f7:e6:9c:76:b7", auth)
          })

        source.failed.foreach(logger.error("couldn't auth", _))

        Source.fromFutureSource(source)
          .map((event: ClientEvent) => {
            println(event)

            event match {
              case _: ClientJoined =>
              case _: ClientLeft =>
            }
          })
          .to(Sink.ignore)
          .run()

        val mdnsService: MDnsService = new MDnsService("_nanoleafapi._tcp")

        mdnsService.query
          .foreach(i => {
            println(i)
          })
          */

    /*
    val service: Service = Service.fromName("_nanoleafapi._tcp")
    val query: Query = Query.createFor(service, Domain.LOCAL)



    query.runOnce()

    val instances = query.runOnceOn(InetAddress.getByName("192.168.1.15"))
    instances.stream.forEach(System.out.println(_))
    */

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
