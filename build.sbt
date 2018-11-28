
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
      "io.circe" %% "circe-generic-extras" % Versions.circe
    )
  )

