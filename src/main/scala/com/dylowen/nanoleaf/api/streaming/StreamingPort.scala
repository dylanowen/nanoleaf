package com.dylowen.nanoleaf.api.streaming

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, UdpConnected}
import akka.util.ByteString
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.utils.ActorStartup
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object StreamingPort {

  def apply(remote: InetSocketAddress)(implicit system: NanoSystem): Future[ActorRef] = {
    import system.actorSystem

    val actor: ActorRef = actorSystem.actorOf(props(remote))

    ActorStartup(actor)
  }

  def props(remote: InetSocketAddress): Props = Props(new StreamingPort(remote))
}

class StreamingPort(remote: InetSocketAddress) extends Actor with ActorStartup with LazyLogging {

  import context.system

  IO(UdpConnected) ! UdpConnected.Connect(self, remote)

  def receive: Receive = preStartup({
    case UdpConnected.Connected ⇒ {
      context.become(ready(sender()))

      triggerStartup()
    }
  })

  def ready(connection: ActorRef): Receive = postStartup({

    case msg: Array[Byte] => {
      connection ! UdpConnected.Send(ByteString(msg))
    }

    case UdpConnected.Received(data) ⇒ {
      logger.warn("we don't expect to receive any data back but got: " + data)
    }

    case UdpConnected.Disconnect ⇒ {
      connection ! UdpConnected.Disconnect
    }

    case UdpConnected.Disconnected ⇒ {
      context.stop(self)
    }
  })
}
