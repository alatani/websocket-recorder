
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.7"
//ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

lazy val tsubaki = (project in file("."))
  .settings(
    name := "tsubaki",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % Versions.akka_http,
      "com.typesafe.akka" %% "akka-stream" % Versions.akka,
      "com.typesafe.akka" %% "akka-actor" % Versions.akka,
      "com.typesafe.akka" %% "akka-stream-contrib" % Versions.akka_contrib,
      "io.circe" %% "circe-generic" % Versions.circe,
      "io.circe" %% "circe-parser" % Versions.circe,
      "io.circe" %% "circe-generic-extras" % Versions.circe,

      "com.google.cloud" % "google-cloud-storage" % Versions.google_cloud_storage,

      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "org.scalatest" %% "scalatest" % "3.0.1"  % Test,
      "org.scalamock" %% "scalamock" % "4.1.0" % Test
    )
  )

