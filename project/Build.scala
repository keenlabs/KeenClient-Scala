import sbt._
import Keys._

object KeenBuild extends Build {
  lazy val root = Project(
    id = "keen",
    base = file("."),
    settings = Project.defaultSettings
  ).settings(
    scalaVersion := "2.10.0"
  )
}