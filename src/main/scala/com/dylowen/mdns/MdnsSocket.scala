package com.dylowen.mdns

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.io.Inet.SocketOption
import akka.io.{IO, Udp}
import akka.util.ByteString
import com.typesafe.scalalogging.{LazyLogging, Logger}
import org.slf4j.LoggerFactory

import scala.collection.immutable

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
private[mdns] object MDnsSocket {

  sealed trait SocketMessage

  case object Ready extends SocketMessage

  case class Send(msg: Array[Byte]) extends SocketMessage

  case class Receive(message: Message) extends SocketMessage

}

private[mdns] class MDnsSocket(controller: ActorRef, address: InetSocketAddress, socketOptions: SocketOption*) extends Actor with LazyLogging {

  import MDnsSocket._
  import context.system

  IO(Udp) ! Udp.Bind(self, null, socketOptions.to[immutable.Traversable])

  def receive: Receive = {
    case _: Udp.Bound => {
      context.become(ready(sender()))

      // let our controller know we're ready
      controller ! Ready
    }
  }

  def ready(connection: ActorRef): Receive = {

    case msg: Array[Byte] => {
      logger.trace("Sending message")

      connection ! Udp.Send(ByteString(msg), address)
    }

    case Udp.Received(data, remote) ⇒ {
      //logger.debug(data.map("0x" + _.toHexString).reduce(_ + " " + _))

      val message: Message = Message(data.toArray)
      logger.debug(s"Received: $message")

      context.parent
    }

    case Udp.Unbind ⇒ {
      connection ! Udp.Unbind
    }

    case Udp.Unbound ⇒ {
      context.stop(self)
    }
  }

  override protected lazy val logger: Logger = Logger(LoggerFactory.getLogger(self.path.toString))
}
