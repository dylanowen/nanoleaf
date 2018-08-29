package com.dylowen.house
package utils

import com.typesafe.scalalogging.LazyLogging
import com.typesafe.sslconfig.ssl.{ConfigSSLContextBuilder, DefaultKeyManagerFactoryWrapper, DefaultTrustManagerFactoryWrapper, KeyManagerFactoryWrapper, SSLConfigSettings, TrustManagerFactoryWrapper}
import io.netty.handler.ssl._
import javax.net.ssl.SSLContext

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

    new ConfigSSLContextBuilder(
      new LoggerBridgeFactory("SSLContextBuilder"),
      sslConfig,
      keyManagerFactory,
      trustManagerFactory
    ).build()
  }
}
