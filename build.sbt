name         := "keenclient-scala"
organization := "io.keen"
description  := "Keen IO SDK/client library for Scala"
homepage     := Some(url("https://github.com/keenlabs/KeenClient-Scala"))

scalaVersion       := "2.11.8"
crossScalaVersions := Seq("2.10.6", scalaVersion.value)
scalacOptions     ++= Seq("-unchecked", "-deprecation", "-feature", "-Xlint")

resolvers ++= Seq(
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
  "spray repo"          at "http://repo.spray.io"
)

libraryDependencies ++= {
  val sprayVersion = "1.3.2"
  Seq(
    "com.typesafe.akka"        %% "akka-actor"      % "2.3.6",
    "com.typesafe"             %  "config"          % "1.2.1",       // 1.3+ is Java 8-only
    "io.spray"                 %% "spray-can"       % sprayVersion,
    "io.spray"                 %% "spray-http"      % sprayVersion,
    "io.spray"                 %% "spray-httpx"     % sprayVersion,
    "io.spray"                 %% "spray-util"      % sprayVersion,
    "net.databinder.dispatch"  %% "dispatch-core"   % "0.11.2",
    "org.clapper"              %% "grizzled-slf4j"  % "1.0.2",
    "org.specs2"               %% "specs2-core"     % "3.7.2"        % "it,test",
    "org.slf4j"                %  "slf4j-simple"    % "1.7.6"        % "it,test"
  )
}

// sbt console convenience
initialCommands in console := "import io.keen.client.scala._"

// ...but skip it in case we've broken the build and want the REPL to find out why!
initialCommands in consoleQuick := ""

// We already pull in sbt-git for sbt-ghpages, so hey why not.
enablePlugins(GitBranchPrompt)

// SBT support for Maven-style integration tests (src/it)
Defaults.itSettings
configs(IntegrationTest)

/**
 * Release Automation
 *
 * Adds the `release` task to do version bumping, artifact publishing, etc.
 * See PUBLISHING.md for a rundown of how to make releases.
 */
releaseCrossBuild    := true
releaseTagComment    := s"Release ${(version in ThisBuild).value}"
releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}"

// For pre-1.0. The default of Next is probably better after that.
releaseVersionBump := sbtrelease.Version.Bump.Minor

// Releases publish signed artifacts. See publishing.sbt for the setup.
releasePublishArtifactsAction := PgpKeys.publishSigned.value

// Tack on ScalaDoc publishing task (configured below) to end of release process
import com.typesafe.sbt.SbtGhPages.GhPagesKeys
releaseProcess += releaseStepTask(GhPagesKeys.pushSite in thisProjectRef.value)

/**
 * Scaladoc Generation
 *
 * This sets up the sbt-site plugin to generate API documentation in
 * `target/site/api/$version` and a small shim to redirect from the site root
 * directly to the API docs, since there is no other site to display.
 *
 * Salient tasks that this adds:
 *
 *   - `makeSite`        - Build the site locally.
 *   - `previewSite`     - Serve site and open in browser.
 *   - `ghpagesPushSite` - Do gh-pages branch dance, commit new docs, and push.
 *                         This is done in a sandbox checkout of the repo, so it
 *                         won't clobber anything dirty in your working dir.
 *
 * The latter '''should''' retain old versions of the API docs so that links to
 * them don't break, but this is currently broken in sbt-ghpages--subscribe here
 * to track fixes: https://github.com/sbt/sbt-ghpages/issues/10
 *
 * Once that's fixed, keep the below doc: :-)
 *
 * Please do not use the `ghpagesCleanSite` task, it wipes out the contents of
 * the gh-pages branch including old versions of the API docs.
 */
autoAPIMappings := true
scalacOptions in (Compile, doc) <++= (version, scmInfo, baseDirectory in ThisBuild) map {
  case (version, Some(scm), basedir) =>
    val sourceTemplate =
      if (version.endsWith("SNAPSHOT"))
        s"${scm.browseUrl}/tree/master€{FILE_PATH}.scala"
      else
        s"${scm.browseUrl}/tree/v${version}€{FILE_PATH}.scala"

    Seq(
      "-doc-title", "Keen IO API Client",
      "-doc-version", version,
      // "-doc-root-content", baseDirectory.value + "/README.md",  // If only Scaladoc supported Markdown...
      "-sourcepath", basedir.getAbsolutePath,
      "-doc-source-url", sourceTemplate,
      "-groups"
    )

  case _ => Seq.empty
}

enablePlugins(SiteScaladocPlugin)
siteSubdirName in SiteScaladoc := s"api/${version.value}"

// Builds static files in src/site-preprocess with variable substitution.
enablePlugins(PreprocessPlugin)
preprocessVars := Map("VERSION" -> version.value)

// Enables easy publishing to project's gh-pages.
ghpages.settings
git.remoteRepo := "git@github.com:keenlabs/KeenClient-Scala.git"

/**
 * Automated source formatting upon compile, for focused code review.
 */
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

SbtScalariform.scalariformSettingsWithIt

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)
