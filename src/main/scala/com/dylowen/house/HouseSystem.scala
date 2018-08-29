package com.dylowen.house

import akka.actor.typed
import akka.actor.typed.{ActorSystem, DispatcherSelector}
import com.dylowen.house.utils.SSLConfigUtils
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.sslconfig.ssl.{SSLConfigFactory, SSLConfigSettings}
import io.netty.handler.ssl.SslContext

import scala.concurrent.ExecutionContextExecutor

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class HouseSystem(private val _actorSystem: typed.ActorSystem[Nothing]) extends LazyLogging {
  @inline
  implicit def typedSystem: ActorSystem[Nothing] = _actorSystem

  @inline
  implicit def executionContext: ExecutionContextExecutor = typedSystem.executionContext

  //@inline
  //implicit def ioExecutionContext: ExecutionContextExecutor = typedSystem.dispatchers.lookup(DispatcherSelector.fromConfig("io-dispatcher"))

  val config: Config = ConfigFactory.load()

  val sslConfig: SSLConfigSettings = SSLConfigFactory.parse(config.getConfig("ssl-config"))

  lazy val sslContext: SslContext = SSLConfigUtils.buildNettySslContext(this)
}
