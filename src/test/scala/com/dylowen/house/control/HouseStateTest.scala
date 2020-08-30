package com.dylowen.house
package control

import java.time.Instant

import com.dylowen.house.unifi.NetworkClient
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.duration._

/**
  * Tests [[HouseState]]
  */
class HouseStateTest extends AnyFlatSpec with Matchers {
  import TestUtils._

  ItName[HouseState] should "find at home phones" in {
    val state: HouseState = new HouseState(
      Set(
        networkClient("gone", lastSeen = Instant.now().minusMillis((20 seconds).toMillis)),
        networkClient("here", lastSeen = Instant.now())
      ),
      Set.empty,
      Set.empty,
      Set.empty,
      10 second
    )

    state.atHomePhones.map(_.mac) shouldNot contain("gone")
    state.atHomePhones.map(_.mac) should contain("here")
  }

  it should "find arrived home phones" in {
    var state: HouseState = new HouseState(
      Set(
        networkClient("reconnecting", lastSeen = Instant.now()),
        networkClient("new", lastSeen = Instant.now()),
        networkClient("other", lastSeen = Instant.now().minusMillis((15 seconds).toMillis))
      ),
      Set.empty,
      Set(
        networkClient("reconnecting", lastSeen = Instant.now()),
        networkClient("gone", lastSeen = Instant.now())
      ),
      Set.empty,
      10 second
    )

    state.atHomePhones.map(_.mac) should contain("reconnecting")
    state.arrivedHomePhones.map(_.mac) shouldNot contain("gone")
    state.arrivedHomePhones.map(_.mac) shouldNot contain("reconnecting")
    state.arrivedHomePhones.map(_.mac) should contain("new")

    state = state.next(
      WifiClients(
        Seq(
          networkClient("newest", lastSeen = Instant.now()),
          networkClient("other", lastSeen = Instant.now())
        ),
        Seq.empty
      )
    )

    state.arrivedHomePhones.map(_.mac) shouldNot contain("gone")
    state.arrivedHomePhones.map(_.mac) shouldNot contain("reconnecting")
    state.arrivedHomePhones.map(_.mac) shouldNot contain("new")
    state.arrivedHomePhones.map(_.mac) shouldNot contain("other")
    state.arrivedHomePhones.map(_.mac) should contain("newest")
  }

  it should "find at home unknown clients" in {
    val state: HouseState = new HouseState(
      Set.empty,
      Set(
        networkClient(
          "notUnknown",
          firstSeen = Instant.now().minusMillis((2 days).toMillis),
          lastSeen = Instant.now()
        ),
        networkClient(
          "old",
          firstSeen = Instant.now().minusMillis((30 seconds).toMillis),
          lastSeen = Instant.now().minusMillis((20 seconds).toMillis)
        ),
        networkClient(
          "new",
          firstSeen = Instant.now(),
          lastSeen = Instant.now()
        )
      ),
      Set.empty,
      Set.empty,
      10 second,
      1 day
    )

    state.atHomeUnknownWirelessClients.map(_.mac) shouldNot contain("notUnknown")
    state.atHomeUnknownWirelessClients.map(_.mac) shouldNot contain("old")
    state.atHomeUnknownWirelessClients.map(_.mac) should contain("new")
  }

  it should "find arrived unknown clients" in {
    var state: HouseState = new HouseState(
      Set.empty,
      Set(
        networkClient(
          "notUnknown",
          firstSeen = Instant.now().minusMillis((2 days).toMillis),
          lastSeen = Instant.now()
        ),
        networkClient(
          "old",
          firstSeen = Instant.now().minusMillis((30 seconds).toMillis),
          lastSeen = Instant.now().minusMillis((20 seconds).toMillis)
        ),
        networkClient(
          "new",
          firstSeen = Instant.now(),
          lastSeen = Instant.now()
        )
      ),
      Set.empty,
      Set(
        networkClient(
          "gone",
          firstSeen = Instant.now(),
          lastSeen = Instant.now()
        )
      ),
      10 second,
      1 day
    )

    state.arrivedUnknownWirelessClients.map(_.mac) shouldNot contain("gone")
    state.arrivedUnknownWirelessClients.map(_.mac) shouldNot contain("notUnknown")
    state.arrivedUnknownWirelessClients.map(_.mac) shouldNot contain("old")
    state.arrivedUnknownWirelessClients.map(_.mac) should contain("new")

    state = state.next(
      WifiClients(
        Seq.empty,
        Seq(
          networkClient(
            "newest",
            firstSeen = Instant.now(),
            lastSeen = Instant.now()
          )
        )
      )
    )

    state.arrivedUnknownWirelessClients.map(_.mac) shouldNot contain("gone")
    state.arrivedUnknownWirelessClients.map(_.mac) shouldNot contain("notUnknown")
    state.arrivedUnknownWirelessClients.map(_.mac) shouldNot contain("old")
    state.arrivedUnknownWirelessClients.map(_.mac) shouldNot contain("new")
    state.arrivedUnknownWirelessClients.map(_.mac) should contain("newest")
  }

  private def networkClient(
      mac: String,
      firstSeen: Instant = Instant.MIN,
      lastSeen: Instant = Instant.MIN
  ): NetworkClient = {
    NetworkClient(
      hostName = None,
      mac,
      ip = None,
      uptime = 1 second,
      first_seen = firstSeen,
      last_seen = lastSeen,
      is_wired = false
    )
  }
}
