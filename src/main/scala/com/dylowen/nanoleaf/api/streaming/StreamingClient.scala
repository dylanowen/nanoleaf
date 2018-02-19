package com.dylowen.nanoleaf.api.streaming

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.io.{IO, UdpConnected}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object StreamingClient {


  def apply(): StreamingClient = new StreamingClient()


}

class StreamingClient private() {

}

