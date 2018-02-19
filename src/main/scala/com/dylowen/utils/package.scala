package com.dylowen

import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
package object utils {
  implicit class EnhancedConfig(config: Config) {
    def getUrl(path: String): URL = {
      new URL(config.getString(path))
    }

    def getFiniteDuration(path: String): FiniteDuration = {
      val duration: Duration = config.getDuration(path)

      // this probably won't overflow since 2^63 - 1 ns is ~= 292 years
      FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS).toCoarsest
    }

    def optionalString(path: String): Option[String] = {
      optional(_.getString)(path)
    }

    def optional[T](fn: Config => String => T)(path: String): Option[T] = {
      if (config.hasPath(path)) {
        Some(fn(config)(path))
      }
      else {
        None
      }
    }
  }
}
