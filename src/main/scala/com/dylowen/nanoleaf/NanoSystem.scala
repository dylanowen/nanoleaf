package com.dylowen.nanoleaf

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.stream.Materializer
import akka.{actor => untyped}
import com.dylowen.utils.SSLConfigUtils
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.sslconfig.ssl.{SSLConfigFactory, SSLConfigSettings}
import io.netty.handler.ssl.SslContext
import javax.net.ssl.SSLContext

import scala.concurrent.ExecutionContextExecutor

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
case class NanoSystem(private val _actorSystem: untyped.ActorSystem,
                      private val _materializer: Materializer) extends LazyLogging {
  @inline
  implicit def typedSystem: ActorSystem[Nothing] = _actorSystem.toTyped

  @inline
  implicit def untypedSystem: untyped.ActorSystem = _actorSystem

  @inline
  implicit def materializer: Materializer = _materializer

  @inline
  implicit def executionContext: ExecutionContextExecutor = typedSystem.executionContext

  val config: Config = ConfigFactory.load()

  val sslConfig: SSLConfigSettings = SSLConfigFactory.parse(config.getConfig("ssl-config"))

  lazy val sslContext: SslContext = SSLConfigUtils.buildNettySslContext(logger)(this)
}
