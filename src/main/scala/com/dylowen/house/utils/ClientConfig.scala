package com.dylowen.house.utils

import com.dylowen.house.HouseSystem
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.{SttpBackend, SttpBackendOptions}
import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.{AsyncHttpClientConfig, DefaultAsyncHttpClientConfig}

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
object ClientConfig {

  @volatile
  private var _backend: Option[SttpBackend[Future, Nothing]] = None

  def backend(implicit nanoSystem: HouseSystem): SttpBackend[Future, Nothing] = {
    if (_backend.isEmpty) {
      synchronized({
        if (_backend.isEmpty) {
          import nanoSystem.executionContext

          _backend = Some(AsyncHttpClientFutureBackend.usingConfig(clientConfig))
        }
      })
    }

    _backend.get
  }

  private def clientConfig(implicit nanoSystem: HouseSystem): AsyncHttpClientConfig = {
    val options: SttpBackendOptions = SttpBackendOptions.Default

    var configBuilder: DefaultAsyncHttpClientConfig.Builder = new DefaultAsyncHttpClientConfig.Builder()
      .setConnectTimeout(options.connectionTimeout.toMillis.toInt)
      .setSslContext(nanoSystem.sslContext)
      .setDisableHttpsEndpointIdentificationAlgorithm(nanoSystem.sslConfig.loose.disableHostnameVerification)
      .setMaxConnections(8)
      .setMaxConnectionsPerHost(4)
      .setPooledConnectionIdleTimeout(100)
      .setConnectionTtl(500)
      .setConnectTimeout((60 seconds).toMillis.toInt)
      .setIoThreadsCount(8)

    configBuilder = options.proxy match {
      case None => configBuilder
      case Some(p) =>
        configBuilder.setProxyServer(new ProxyServer.Builder(p.host, p.port).build())
    }

    configBuilder
      .setSslContext(nanoSystem.sslContext)
      .build()
  }
}
