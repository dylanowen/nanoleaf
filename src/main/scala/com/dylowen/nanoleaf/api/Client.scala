package com.dylowen.nanoleaf.api

import scala.concurrent.Future

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
object Client {
  def apply: Future[Client] = {
    new Client()

    null
  }
}

class Client {

}
