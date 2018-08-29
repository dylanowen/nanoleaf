package com.dylowen.house.utils

import com.typesafe.scalalogging.Logger
import com.typesafe.sslconfig.util.{LoggerFactory, NoDepsLogger}
import org.slf4j.{LoggerFactory => SlfLoggerFactor}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
class LoggerBridgeFactory(name: String) extends LoggerFactory {

  private val bridge: NoDepsLogger = new LoggerBridge(name)

  override def apply(clazz: Class[_]): NoDepsLogger = bridge

  override def apply(name: String): NoDepsLogger = bridge
}

class LoggerBridge(name: String) extends NoDepsLogger {

  private val sslContextLogger: Logger = Logger(SlfLoggerFactor.getLogger(name))

  override def isDebugEnabled: Boolean = sslContextLogger.underlying.isDebugEnabled

  override def debug(msg: String): Unit = sslContextLogger.debug(msg)

  override def info(msg: String): Unit = sslContextLogger.info(msg)

  override def warn(msg: String): Unit = sslContextLogger.warn(msg)

  override def error(msg: String): Unit = sslContextLogger.error(msg)

  override def error(msg: String, throwable: Throwable): Unit = sslContextLogger.error(msg, throwable)

}
