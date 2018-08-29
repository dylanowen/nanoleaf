enablePlugins(JavaServerAppPackaging)
enablePlugins(DebianPlugin)
enablePlugins(SystemdPlugin)

name := "nanoleaf"
organization := "com.dylowen"
maintainer := "Dylan Owen"
version := "0.1"

scalaVersion := "2.12.6"

scalacOptions in ThisBuild ++= Seq(
  "-feature",
  "-deprecation",
  "-language:postfixOps"
)

libraryDependencies ++= Dependencies.Akka
libraryDependencies += Dependencies.TypesafeConfig
libraryDependencies += Dependencies.JmDNS
libraryDependencies ++= Dependencies.Sttp
libraryDependencies ++= Dependencies.Circe
libraryDependencies ++= Dependencies.Logging
libraryDependencies ++= Dependencies.Jackson

mainClass in Compile := Some("com.dylowen.house.HouseApp")

packageSummary := "House-Controller"
packageDescription := "House Controller"
daemonUser in Linux := "pi"