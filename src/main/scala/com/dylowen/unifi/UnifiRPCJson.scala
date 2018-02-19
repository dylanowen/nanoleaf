package com.dylowen.unifi

import com.dylowen.utils.UtilJsonSupport
import spray.json.{JsValue, RootJsonFormat}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
private[unifi] final case class UnifiRPCJson(data: JsValue, meta: JsValue)

private[unifi] trait UnifiRPCJsonSupport extends UtilJsonSupport {
  implicit val unifiRPCJsonFormat: RootJsonFormat[UnifiRPCJson] = jsonFormat2(UnifiRPCJson)
}
