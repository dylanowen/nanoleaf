package com.dylowen.house

import akka.actor.ActorSystem
import akka.event.DummyClassForStringSources
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.{SttpBackend, SttpBackendOptions}
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.sslconfig.akka.util.AkkaLoggerBridge
import com.typesafe.sslconfig.util.{LoggerFactory, NoDepsLogger}
import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.{AsyncHttpClientConfig, DefaultAsyncHttpClientConfig}

import scala.concurrent.Future

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
package object unifi extends LazyLogging {

  @volatile
  private var _unifiClientBackend: Option[SttpBackend[Future, Nothing]] = None


  def UnifiClientBackend(implicit nanoSystem: HouseSystem): SttpBackend[Future, Nothing] = {
    if (_unifiClientBackend.isEmpty) {
      synchronized({
        if (_unifiClientBackend.isEmpty) {

          _unifiClientBackend = Some(AsyncHttpClientFutureBackend.usingConfig(clientConfig))
        }
      })
    }

    _unifiClientBackend.get
  }

  private def clientConfig(implicit nanoSystem: HouseSystem): AsyncHttpClientConfig = {
    val options: SttpBackendOptions = SttpBackendOptions.Default

    var configBuilder: DefaultAsyncHttpClientConfig.Builder = new DefaultAsyncHttpClientConfig.Builder()
      .setConnectTimeout(options.connectionTimeout.toMillis.toInt)
      .setSslContext(nanoSystem.sslContext)

    configBuilder = options.proxy match {
      case None => configBuilder
      case Some(p) =>
        configBuilder.setProxyServer(new ProxyServer.Builder(p.host, p.port).build())
    }

    configBuilder
      .setSslContext(nanoSystem.sslContext)
      .build()
  }

  private final class UnifiLoggerFactory(system: ActorSystem) extends LoggerFactory {
    override def apply(clazz: Class[_]): NoDepsLogger = new AkkaLoggerBridge(system.eventStream, clazz)

    override def apply(name: String): NoDepsLogger = new AkkaLoggerBridge(system.eventStream, name, classOf[DummyClassForStringSources])
  }

}
