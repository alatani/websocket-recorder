import Dependencies._

ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.7"
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

lazy val tsubaki = (project in file("."))
  .settings(
    name := "tsubaki",
    libraryDependencies ++= Seq(
      `akka-http`,
      `circe-generic`,
      `circe-parser`,
      `circe-extra`
    )
  )

