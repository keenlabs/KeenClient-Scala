addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.0.0")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")
