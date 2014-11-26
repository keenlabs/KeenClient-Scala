organization := "keen"

name := "keenclient-scala"

version := "1.0.2"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4")

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies += "io.spray" %% "spray-can" % "1.3.2"

libraryDependencies += "io.spray" %% "spray-http" % "1.3.2"

libraryDependencies += "io.spray" %% "spray-httpx" % "1.3.2"

libraryDependencies += "io.spray" %% "spray-util" % "1.3.2"

libraryDependencies += "io.spray" %% "spray-can" % "1.3.2"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.6"

libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"

libraryDependencies += "org.clapper" %% "grizzled-slf4j" % "1.0.2"

libraryDependencies += "org.specs2" %% "specs2" % "2.4.13" % "test"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.6" % "test"
