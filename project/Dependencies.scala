import sbt._

/**
  * @author dylan.owen
  * @since Jul-2018
  */
object Dependencies {

  object Versions {
    val Akka: String = "2.6.8"
    val AkkaHttp: String = "10.2.0"

    val TypesafeConfig: String = "1.3.1"

    val Hola: String = "0.2.2"

    val Sttp: String = "1.7.2"

    val Circe: String = "0.13.0"

    val Log4j: String = "2.16.0"
    val Slf4j: String = "1.7.30"

    val Jackson: String = "2.8.8"

    val ScalaTest: String = "3.2.2"
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
    "org.scalatest" %% "scalatest" % Versions.ScalaTest % Test,
    "org.scalatestplus" %% "mockito-3-4" % s"${Versions.ScalaTest}.0" % Test
  )

  lazy val Mockito: ModuleID = "org.mockito" % "mockito-core" % "3.5.7" % Test
}
