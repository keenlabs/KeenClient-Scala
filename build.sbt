name := "keenclient-scala"

organization := "io.keen"

description := "Keen IO SDK/client library for Scala"

homepage := Some(url("https://github.com/keenlabs/KeenClient-Scala"))

version := "0.5.0-SNAPSHOT"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers ++= Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
  "spray repo"          at "http://repo.spray.io"
)

libraryDependencies ++= {
  val sprayVersion = "1.3.2"
  Seq(
    "com.typesafe.akka"        %% "akka-actor"      % "2.3.6",
    "com.typesafe"             %  "config"          % "1.2.1",
    "io.spray"                 %% "spray-can"       % sprayVersion,
    "io.spray"                 %% "spray-http"      % sprayVersion,
    "io.spray"                 %% "spray-httpx"     % sprayVersion,
    "io.spray"                 %% "spray-util"      % sprayVersion,
    "net.databinder.dispatch"  %% "dispatch-core"   % "0.11.2",
    "org.clapper"              %% "grizzled-slf4j"  % "1.0.2",
    "org.specs2"               %% "specs2"          % "2.4.13"       % "test",
    "org.slf4j"                %  "slf4j-simple"    % "1.7.6"        % "test"
  )
}

// sbt console convenience
initialCommands in console := "import io.keen.client.scala._"

// ...but skip it in case we've broken the build and want the REPL to find out why!
initialCommands in consoleQuick := ""

