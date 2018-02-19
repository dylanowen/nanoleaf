package com.dylowen.unifi

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.dylowen.nanoleaf.NanoSystem
import spray.json.JsArray
import spray.{json => Json}

import scala.concurrent.Future


/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Feb-2018
  */
object GetDevices extends UnifiRPCJsonSupport with DeviceJsonSupport {

  def apply(auth: UnifiAuthorization, site: String = "default")
           (implicit nanoSystem: NanoSystem): Future[Seq[Device]] = {
    import nanoSystem.{actorSystem, executionContext, materializer}

    val request: HttpRequest = auth.request(path = "/api/s/default/stat/sta")

    Http().singleRequest(request)
      .flatMap((response: HttpResponse) => {
        Unmarshal(response.entity).to[UnifiRPCJson]
      })
      .map((rpcJson: UnifiRPCJson) => {
        rpcJson.data match {
          case JsArray(elements) => elements.map(v => {
            //println(v)
            v.convertTo[Device]
          })
          case _ => Json.deserializationError("expected an array")
        }
      })
  }
}


