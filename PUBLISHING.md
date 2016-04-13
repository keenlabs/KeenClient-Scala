Release Process
---------------

Following is the procedure for publishing new releases. If you're making your first release, see the *Publishing Setup* section below first.

* Ensure `CHANGELOG.md` is up-to-date and you're on `master` with clean git index.
* Run `sbt release`. You will be prompted for versions, with likely defaults filled.
* Release the published artifacts for public consumption:
    * Visit [Sonatype](https://oss.sonatype.org/#stagingRepositories) and "Close" the repo
    * Await the above to finish, fix anything that breaks if it breaks
    * Go to Sonatype again and "Release" the repo
    * It should show up on Maven Central at some point after that.

It's possible to run `sbt release` in a non-interactive manner, but you probably want to pause to think about the SemVer implications of the release and verify that what automation suggests is the appropriate choice.

Metadata for our artifacts and other nexus details are set in the `publishing.sbt` file.

### The sbt release Magic ###

Here is what `sbt release` does, in a nutshell:

1. Makes sure git is clean and no `SNAPSHOT` dependencies are declared in the project.
1. Prompts for the number of the version to be released, and the subsequent development version to set after the release.
1. Runs unit tests, aborting release if there are failures.
1. Writes the release version to `version.sbt` and commits that.
1. Tags that release commit.
1. Publishes the cross-version artifacts to Maven Central.
1. Writes the next development `SNAPSHOT` version to `version.sbt` and commits that.
1. Pushes to GitHub.
1. Publishes Scaladoc to <https://keenlabs.github.io/KeenClient-Scala/>.

This process can be completely customized, see [sbt-release](https://github.com/sbt/sbt-release).

Publishing Setup
----------------

We host artifacts for this library on Maven Central. [Here are the official docs for that](http://central.sonatype.org/pages/ossrh-guide.html).

**TL;DR** you'll need to do the following to publish releases:

1. Create an account on [the Sonatype JIRA](http://issues.sonatype.org/).
2. Request publish access for our `io.keen` namespace by commenting on [our project's ticket](https://issues.sonatype.org/browse/OSSRH-12955).
3. Log in with the same Sonatype account at <http://oss.sonatype.org> and create an access token:
    - Navigate to your profile
    - Select user token from the profile settings dropdown
    - Click access user token
4. Create a [credentials file] at `~/.ivy2/.credentials` that looks like this, using your username and the token from the previous step:

         realm=Sonatype Nexus Repository Manager
         host=oss.sonatype.org
         user=<username>
         password=<token>

5. Finally, releases must be PGP-signed, so if you have never created a GPG key for your email address and distributed it to keyservers, you should do so now. `sbt-pgp` (already installed in this project) can help:

         $ sbt
         > set pgpReadOnly := false
         > pgp-cmd gen-key
         > pgp-cmd send-key <email addr> hkp://keyserver.ubuntu.com

   See <http://www.scala-sbt.org/sbt-pgp/usage.html> if you need further info.

[credentials file]: http://www.scala-sbt.org/0.13/docs/Publishing.html#Credentials
