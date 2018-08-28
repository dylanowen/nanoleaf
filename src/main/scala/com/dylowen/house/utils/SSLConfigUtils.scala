package com.dylowen.house
package utils

import com.typesafe.scalalogging.{LazyLogging, Logger}
import com.typesafe.sslconfig.ssl.{ConfigSSLContextBuilder, DefaultKeyManagerFactoryWrapper, DefaultTrustManagerFactoryWrapper, KeyManagerFactoryWrapper, SSLConfigSettings, TrustManagerFactoryWrapper}
import com.typesafe.sslconfig.util.{LoggerFactory, NoDepsLogger}
import io.netty.handler.ssl._
import javax.net.ssl.SSLContext
import org.slf4j.{LoggerFactory => SlfLoggerFactor}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object SSLConfigUtils extends LazyLogging {

  def buildNettySslContext(implicit nanoSystem: HouseSystem): SslContext = {
    val jdkSslContext: SSLContext = buildSSLContext

    new JdkSslContext(jdkSslContext, true, ClientAuth.NONE)
  }

  def buildSSLContext(implicit nanoSystem: HouseSystem): SSLContext = {
    val sslConfig: SSLConfigSettings = nanoSystem.sslConfig

    val keyManagerAlgorithm: String = sslConfig.keyManagerConfig.algorithm
    val keyManagerFactory: KeyManagerFactoryWrapper = new DefaultKeyManagerFactoryWrapper(keyManagerAlgorithm)

    val trustManagerAlgorithm: String = sslConfig.trustManagerConfig.algorithm
    val trustManagerFactory: TrustManagerFactoryWrapper = new DefaultTrustManagerFactoryWrapper(trustManagerAlgorithm)

    new ConfigSSLContextBuilder(SSLLoggerFactory, sslConfig, keyManagerFactory, trustManagerFactory).build()
  }

  private object SSLLoggerFactory extends LoggerFactory {
    override def apply(clazz: Class[_]): NoDepsLogger = SSLLoggerBridge

    override def apply(name: String): NoDepsLogger = SSLLoggerBridge
  }

  private object SSLLoggerBridge extends NoDepsLogger {

    private val sslContextLogger: Logger = Logger(SlfLoggerFactor.getLogger("SSLContextBuilder"))

    override def isDebugEnabled: Boolean = sslContextLogger.underlying.isDebugEnabled

    override def debug(msg: String): Unit = sslContextLogger.debug(msg)

    override def info(msg: String): Unit = sslContextLogger.info(msg)

    override def warn(msg: String): Unit = sslContextLogger.warn(msg)

    override def error(msg: String): Unit = sslContextLogger.error(msg)

    override def error(msg: String, throwable: Throwable): Unit = sslContextLogger.error(msg, throwable)

  }
}
