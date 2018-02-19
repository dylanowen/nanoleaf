package com.dylowen.utils

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.dylowen.nanoleaf.NanoSystem

import scala.concurrent.Future

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object ActorStartup {

  private case object Startup

  def apply(actorRef: ActorRef, timeout: Timeout = Timeout(1, TimeUnit.MINUTES))
           (implicit system: NanoSystem): Future[ActorRef] = {
    import system.executionContext

    // ask our actor about startup
    actorRef.ask(Startup)(timeout)
      .map(_ => actorRef)
  }
}

trait ActorStartup {
  this: Actor =>

  import ActorStartup._

  private var startupCallback: Option[ActorRef] = None

  def preStartup(receive: Receive): Receive = ({
    case Startup => startupCallback = Some(sender())
  }: Receive).orElse(receive)

  def triggerStartup(): Unit = {
    notifyReady()
  }

  def postStartup(receive: Receive): Receive = ({
    case Startup => notifyReady(Some(sender()))
  }: Receive).orElse(receive)

  private def notifyReady(callback: Option[ActorRef] = startupCallback): Unit = {
    // let people know our port is ready
    callback.foreach(_ ! "ready")

    startupCallback = None
  }
}
