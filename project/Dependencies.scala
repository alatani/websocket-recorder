import sbt._

object Versions {
  val circe = "0.10.0"
  val akka = "10.1.5"
}

object Dependencies {

  val `akka-http` =  "com.typesafe.akka" %% "akka-http" % Versions.akka

  val `circe-generic` = "io.circe" %% "circe-generic" % Versions.circe
  val `circe-parser` = "io.circe" %% "circe-parser" % Versions.circe
  val `circe-extra` = "io.circe" %% "circe-generic-extras" % Versions.circe
}
