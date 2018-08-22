package com.dylowen.utils

import akka.actor.ActorSystem
import com.dylowen.nanoleaf.NanoSystem
import com.typesafe.scalalogging.{LazyLogging, Logger}
import com.typesafe.sslconfig.ssl.{ConfigSSLContextBuilder, DefaultKeyManagerFactoryWrapper, DefaultTrustManagerFactoryWrapper, KeyManagerFactoryWrapper, SSLConfigSettings, TrustManagerFactoryWrapper}
import com.typesafe.sslconfig.util.{LoggerFactory, NoDepsLogger}
import io.netty.handler.ssl._
import javax.net.ssl.SSLContext

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object SSLConfigUtils extends LazyLogging {

  def buildNettySslContext(wrappedLogger: Logger = logger)(implicit nanoSystem: NanoSystem): SslContext = {
    val jdkSslContext: SSLContext = buildSSLContext(wrappedLogger)

    new JdkSslContext(jdkSslContext, true, ClientAuth.NONE)
  }

  def buildSSLContext(wrappedLogger: Logger = logger)(implicit nanoSystem: NanoSystem): SSLContext = {
    val sslConfig: SSLConfigSettings = nanoSystem.sslConfig

    val keyManagerAlgorithm: String = sslConfig.keyManagerConfig.algorithm
    val keyManagerFactory: KeyManagerFactoryWrapper = new DefaultKeyManagerFactoryWrapper(keyManagerAlgorithm)

    val trustManagerAlgorithm: String = sslConfig.trustManagerConfig.algorithm
    val trustManagerFactory: TrustManagerFactoryWrapper = new DefaultTrustManagerFactoryWrapper(trustManagerAlgorithm)

    new ConfigSSLContextBuilder(new SSLLoggerFactory(wrappedLogger), sslConfig, keyManagerFactory, trustManagerFactory).build()
  }

  private final class SSLLoggerFactory(wrappedLogger: Logger) extends LoggerFactory {
    override def apply(clazz: Class[_]): NoDepsLogger = new SSLLoggerBridge(wrappedLogger)

    override def apply(name: String): NoDepsLogger = new SSLLoggerBridge(wrappedLogger)
  }

  private class SSLLoggerBridge(wrappedLogger: Logger) extends NoDepsLogger {

    override def isDebugEnabled: Boolean = wrappedLogger.underlying.isDebugEnabled

    override def debug(msg: String): Unit = wrappedLogger.debug(msg)

    override def info(msg: String): Unit = wrappedLogger.info(msg)

    override def warn(msg: String): Unit = wrappedLogger.warn(msg)

    override def error(msg: String): Unit = wrappedLogger.error(msg)

    override def error(msg: String, throwable: Throwable): Unit = wrappedLogger.error(msg, throwable)

  }

}
