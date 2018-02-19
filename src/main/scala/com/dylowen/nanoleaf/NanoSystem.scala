package com.dylowen.nanoleaf

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.dylowen.unifi.UnifiConfig
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContextExecutor

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class NanoSystem(private val _actorSystem: ActorSystem,
                      private val _materializer: Materializer) {
  @inline
  implicit def actorSystem: ActorSystem = _actorSystem

  @inline
  implicit def materializer: Materializer = _materializer

  @inline
  implicit def executionContext: ExecutionContextExecutor = _materializer.executionContext

  val config: Config = ConfigFactory.load()
}
