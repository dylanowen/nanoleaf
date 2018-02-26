
name := "nanoleaf"

version := "0.1"

scalaVersion := "2.12.4"

val akkaVersion: String = "2.5.9"
val akkaHttpVersion: String = "10.0.11"
val log4jVersion: String = "2.8.2"
val jacksonVersion: String = "2.8.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

  "com.typesafe" % "config" % "1.3.1",

  "net.straylightlabs" % "hola" % "0.2.2" excludeAll(
    ExclusionRule(organization = "ch.qos.logback")
  ),

  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion
)