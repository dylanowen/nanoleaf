package com.dylowen.unifi

import com.dylowen.utils.UtilJsonSupport
import io.circe
import spray.json.{JsValue, RootJsonFormat}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
private[unifi] final case class UnifiRPCJson(data: circe.Json, meta: circe.Json)

/*
private[unifi] trait UnifiRPCJsonSupport extends UtilJsonSupport {
  implicit val unifiRPCJsonFormat: RootJsonFormat[UnifiRPCJson] = jsonFormat2(UnifiRPCJson)
}
*/
