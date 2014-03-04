organization := "keen"

name := "keenclient-scala"

version := "0.0.1"

scalaVersion := "2.10.3"

crossScalaVersions := Seq("2.10.0", "2.10.1", "2.10.2")

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies += "io.spray" % "spray-can" % "1.2.0"

libraryDependencies += "io.spray" % "spray-http" % "1.2.0"

libraryDependencies += "io.spray" % "spray-httpx" % "1.2.0"

libraryDependencies += "io.spray" % "spray-util" % "1.2.0"

libraryDependencies += "io.spray" % "spray-can" % "1.2.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.4"

libraryDependencies += "org.clapper" %% "grizzled-slf4j" % "1.0.1"

libraryDependencies += "org.specs2" %% "specs2" % "2.3.7" % "test"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.5" % "test"
