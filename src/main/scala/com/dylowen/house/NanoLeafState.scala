package com.dylowen.house

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.dylowen.nanoleaf.NanoSystem
import com.dylowen.nanoleaf.api.{NanoLeafBrightness, NanoLeafClient, NanoLeafHouse}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object NanoLeafState {

  private val RefreshInterval: FiniteDuration = 5 minutes

  case class CacheRefresh(client: Future[NanoLeafClient], lastUpdate: Instant = Instant.now()) {
    def canRefresh(refreshInterval: FiniteDuration): Boolean = {
      val maxDateAllowingUpdate: Instant = Instant.now.minus(refreshInterval.toSeconds, ChronoUnit.SECONDS)

      lastUpdate.isBefore(maxDateAllowingUpdate)
    }
  }

  private val cache: AtomicReference[CacheRefresh] = new AtomicReference[CacheRefresh](CacheRefresh(Future.failed(new UnsupportedOperationException("We have never refreshed our cache")), Instant.MIN))

  def apply()(implicit nanoSystem: NanoSystem): Flow[Any, NanoLeafState, NotUsed] = Flow[Any].mapAsync(1)((_: Any) => {
    import nanoSystem.executionContext

    getClient
      .flatMap((client: NanoLeafClient) => {
        val onFuture: Future[Boolean] = client.isOn
        val brightnessFuture: Future[NanoLeafBrightness] = client.brightness

        for {
          on <- onFuture
          brightness <- brightnessFuture
        } yield NanoLeafState(brightness, on)
      })
  })

  def getClient(implicit nanoSystem: NanoSystem): Future[NanoLeafClient] = {
    val house: NanoLeafHouse = new NanoLeafHouse()
    val lastRefresh: CacheRefresh = cache.get()

    if (lastRefresh.canRefresh(RefreshInterval)) {
      val promise: Promise[NanoLeafClient] = Promise()

      if (cache.compareAndSet(lastRefresh, CacheRefresh(promise.future))) {
        promise.completeWith(house.getClients)

        promise.future
      }
      else {
        cache.get().client
      }
    }
    else {
      lastRefresh.client
    }
  }
}

case class NanoLeafState(brightness: NanoLeafBrightness, on: Boolean)
