/*
 * Maven Central Publishing - Sonatype OSSRH
 *
 * For reference, see:
 *   http://www.scala-sbt.org/0.13/docs/Publishing.html
 *   http://www.scala-sbt.org/0.13/docs/Using-Sonatype.html
 */
import SonatypeKeys._

sonatypeSettings

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

organizationName := "Keen IO"
organizationHomepage := Some(url("https://keen.io/"))

publishMavenStyle := true
publishArtifact in Test := false
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/keenlabs/KeenClient-Scala"),
    "scm:git:https://github.com/keenlabs/KeenClient-Scala.git",
    Some("scm:git:git@github.com:keenlabs/KeenClient-Scala.git")
  )
)

// Central doesn't allow any external repository sources.
pomIncludeRepository := { _ => false }
pomExtra :=
  <developers>
    <developer>
      <id>keenlabs</id>
      <name>Keen IO</name>
      <email></email>
    </developer>
  </developers>

