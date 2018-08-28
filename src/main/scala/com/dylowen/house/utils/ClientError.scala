package com.dylowen.house.utils

import com.softwaremill.sttp.Response
import com.typesafe.scalalogging.Logger

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Aug-2018
  */
case class ClientError(message: Option[String] = None,
                       response: Option[Response[_]] = None,
                       throwable: Option[Throwable] = None) {
  override def toString: String = {
    val messageString: String = message.map(_ + " ").getOrElse("")
    val responseString: String = response.map("response: " + _.toString + " ").getOrElse("")
    val throwableString: String = throwable.map(_.toString).getOrElse("")

    s"$messageString$responseString$throwableString"
  }

  def logError(logMessage: String, logger: Logger): Unit = {
    val messageString: String = message.map(_ + " ").getOrElse("")
    val responseString: String = response.map("response: " + _.toString + " ").getOrElse("")

    if (throwable.isDefined) {
      logger.underlying.error(s"$logMessage: $messageString$responseString", throwable.get)
    }
    else {
      logger.error(s"$logMessage: $messageString$responseString")
    }

  }
}
