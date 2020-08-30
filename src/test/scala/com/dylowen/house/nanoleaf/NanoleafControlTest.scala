package com.dylowen.house.nanoleaf

import akka.actor.typed
import com.dylowen.house.TestUtils.ItName
import com.dylowen.house.control.HouseState
import com.dylowen.house.nanoleaf.NanoleafControl.InternalState
import com.dylowen.house.nanoleaf.api.Brightness
import com.dylowen.house.nanoleaf.mdns.NanoleafAddress
import com.dylowen.house.unifi.NetworkClient
import com.dylowen.house.{HouseSystem, MockitoSpec}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

/**
  * Tests [[NanoleafControl]]
  */
class NanoleafControlTest extends AnyFlatSpec with Matchers with MockitoSpec {

  private val actorSystem: typed.ActorSystem[Nothing] = mock[typed.ActorSystem[Nothing]]
  implicit private val system: HouseSystem = HouseSystem(actorSystem)

  private val address: NanoleafAddress = mock[NanoleafAddress]
  private val config: NanoleafConfig = mock[NanoleafConfig]
  private val phone: NetworkClient = mock[NetworkClient]

  ItName[NanoleafControl] should "turn off the lights" in {
    val control: NanoleafControl = new NanoleafControl(address, config)

    val houseState: HouseState = mock[HouseState]
    val internalState: InternalState = mock[InternalState]

    doReturn(Set.empty).when(houseState).atHomePhones
    control
      .getNextStateActions(
        NanoleafState(Brightness(100, 100, 100), true, ""),
        houseState,
        internalState
      )
      .action shouldBe Some(LightOff)
  }

  it should "turn on the lights" in {
    val control: NanoleafControl = new NanoleafControl(address, config)

    val houseState: HouseState = mock[HouseState]
    val internalState: InternalState = mock[InternalState]
    doReturn(LightOff).when(internalState).action

    doReturn(Set(phone)).when(houseState).atHomePhones
    control
      .getNextStateActions(
        NanoleafState(Brightness(100, 100, 100), false, ""),
        houseState,
        internalState
      )
      .action shouldBe Some(LightOn)
  }

}
