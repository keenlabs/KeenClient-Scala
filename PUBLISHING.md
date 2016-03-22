# Publishing

We host artifacts for this library on Maven Central. [Here are the official docs for that](http://central.sonatype.org/pages/ossrh-guide.html).

Those are kinda complicated, so you'll need the following to publish to it:

* An account on the Sonatype JIRA
* If you've never published to the io.keen namespace you'll need to open a ticket to get yourself added to that. [Here's mine](https://issues.sonatype.org/browse/OSSRH-12955).
* Generate a local GPG key and publish it to a keyserver. I ended up having to export mine ascii armored and uploading it by hand to `keyserver.ubuntu.com`.

# Actual Release Process

* Update `CHANGELOG.md`
* Update `version` in `build.sbt`
* Commit, tag, and push
* Publish the artifacts:
    * Run `sbt +publishSigned`
    * Visit [Sonatype](https://oss.sonatype.org/#stagingRepositories) and "Close" the repo
    * Await the above to finish, fix anything that breaks if it breaks
    * Go to Sonatype again and "Release" the repo
    * It should show up on Maven Central at some point after that.
* Publish new Scaladoc API documentation:

        $ sbt ghpagesPushSite

  If you're feeling prudent, you can run `sbt previewSite` first to take a look.
