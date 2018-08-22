
name := "nanoleaf"
organization in ThisBuild := "com.dylowen"
version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Dependencies.Akka
libraryDependencies ++= Dependencies.AkkaHttp
libraryDependencies += Dependencies.TypesafeConfig
libraryDependencies += Dependencies.JmDNS
libraryDependencies ++= Dependencies.Sttp
libraryDependencies ++= Dependencies.Circe
libraryDependencies ++= Dependencies.Logging
libraryDependencies ++= Dependencies.Jackson