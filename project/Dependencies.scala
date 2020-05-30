import sbt._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Jul-2018
  */
object Dependencies {

  object Versions {
    val Akka: String = "2.5.14"
    val AkkaHttp: String = "10.1.3"

    val TypesafeConfig: String = "1.3.1"

    val Hola: String = "0.2.2"

    val Sttp: String = "1.2.3"

    val Circe: String = "0.9.3"

    val Log4j: String = "2.8.2"
    val Slf4j: String = "1.7.25"

    val Jackson: String = "2.8.8"
  }

  lazy val Akka: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % Versions.Akka,
    "com.typesafe.akka" %% "akka-stream" % Versions.Akka
  )

  lazy val AkkaHttp: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http" % Versions.AkkaHttp,
    "com.typesafe.akka" %% "akka-http-spray-json" % Versions.AkkaHttp
  )

  lazy val TypesafeConfig: ModuleID = "com.typesafe" % "config" % Versions.TypesafeConfig

  lazy val Hola: ModuleID = "net.straylightlabs" % "hola" % Versions.Hola excludeAll
    ExclusionRule(organization = "ch.qos.logback")

  lazy val JmDNS: ModuleID = "org.jmdns" % "jmdns" % "3.5.4"

  lazy val Sttp: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp" %% "core" % Versions.Sttp,
    "com.softwaremill.sttp" %% "async-http-client-backend-future" % Versions.Sttp,
    "com.softwaremill.sttp" %% "circe" % Versions.Sttp
  )

  lazy val Circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core" % Versions.Circe,
    "io.circe" %% "circe-generic" % Versions.Circe,
    "io.circe" %% "circe-parser" % Versions.Circe
  )

  lazy val Logging: Seq[ModuleID] = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    "org.apache.logging.log4j" % "log4j-api" % Versions.Log4j,
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % Versions.Log4j,
    "org.slf4j" % "slf4j-api" % Versions.Slf4j
  )

  lazy val ScalaTest: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % "3.1.2",
    "org.scalatest" %% "scalatest" % "3.1.2" % "test"
  )
}
