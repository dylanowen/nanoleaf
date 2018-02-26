package com.dylowen.mdns

import java.net._

import com.typesafe.scalalogging.LazyLogging
import net.straylightlabs.hola.dns.Domain
import net.straylightlabs.hola.sd.{Instance, Query, Service}

import scala.collection.JavaConverters._
import scala.collection.{immutable, mutable}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jan-2018
  */
class MDnsService(val serviceName: String, val searchLoopback: Boolean = false)
                 (implicit executionContext: ExecutionContext) extends LazyLogging {

  val service: Service = Service.fromName(serviceName)

  def query: Future[immutable.Set[Instance]] = {
    val queries: Future[immutable.Set[Try[immutable.Set[Instance]]]] = Future.sequence(getValidInetAddresses
      .map(runQuery(_).transform((r: Try[immutable.Set[Instance]]) => Success(r))))

    // log the entire query if it failed
    queries.failed.foreach(logger.error("All our queries failed", _))

    // log the individual failed queries
    queries.foreach(_.foreach((result: Try[immutable.Set[Instance]]) => {
      result.failed.foreach(logger.warn("A query failed", _))
    }))

    // get our result
    queries.map(_.collect({
        case Success(instances) => instances
      })
      .fold(Set())(_ ++ _))
  }

  private def runQuery(address: InetAddress): Future[immutable.Set[Instance]] = {
    Future({
      logger.info("Running query for address: " + address)

      Query.createFor(service, Domain.LOCAL).runOnceOn(address)
        .asScala.toSet
    })
  }

  private def getValidInetAddresses: immutable.Set[InetAddress] = {
    val addresses: immutable.Set[InetAddress] = getValidInterfaces
      .flatMap(_.getInetAddresses.asScala)
      .toSet

    if (logger.underlying.isTraceEnabled && addresses.nonEmpty) {
      logger.info("Found valid addresses: " + addresses.map(_.getHostAddress).reduce(_ + ", " + _))
    }

    addresses
  }

  private def getValidInterfaces: Seq[NetworkInterface] = {
    val validInterfaces: mutable.Builder[NetworkInterface, immutable.Seq[NetworkInterface]] = immutable.Seq.newBuilder
    val seen: mutable.Set[Array[Byte]] = mutable.Set()

    NetworkInterface.getNetworkInterfaces.asScala
      .filter((interface: NetworkInterface) => {
        // filter out invalid interfaces
        //interface.isUp && !interface.isVirtual && interface.supportsMulticast && (searchLoopback || !interface.isLoopback)

        // we don't have a good way to know what interfaces are "valid" so just grab patterns we know
        interface.getDisplayName.indexOf("en") == 0
      })
      .foreach((interface: NetworkInterface) => {
        // make sure our interfaces all have distinct mac addresses
        val hardwareAddress: Array[Byte] = interface.getHardwareAddress

        if (hardwareAddress == null || !seen.contains(hardwareAddress)) {
          seen += hardwareAddress
          validInterfaces += interface
        }
      })

    validInterfaces.result()
  }
}
