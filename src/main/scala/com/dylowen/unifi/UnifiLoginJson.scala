package com.dylowen.unifi

import com.dylowen.utils.UtilJsonSupport
import spray.json.RootJsonFormat

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
private[unifi] final case class UnifiLoginJson(username: String, password: String)

private[unifi] trait UnifiLoginJsonSupport extends UtilJsonSupport {
  implicit val unifiLoginJsonFormat: RootJsonFormat[UnifiLoginJson] = jsonFormat2(UnifiLoginJson)
}
