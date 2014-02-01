# keen-scala

*Note*: This library is currently in development and does not implement all of the features of the Keen API.
It's safe to use for single-event publishing. Bulk publishing and other API features will be added over time.

keen-scala is based on the [dispatch](http://dispatch.databinder.net/Dispatch.html)
asynchronous HTTP library. Therefore, all of the returned values are
`Future[Response]`.

The returned object is a [Response](http://sonatype.github.io/async-http-client/apidocs/reference/com/ning/http/client/Response.html)
from the async-http-client library. Normally you'll want to use `getResponseBody`
to get the response but you can also check `getStatusCode` to verify something
didn't go awry.

## Dependencies

Depends on [dispatch](http://dispatch.databinder.net/Dispatch.html) and
[grizzled-slf4j](http://software.clapper.org/grizzled-slf4j/). It's compiled for
scala 2.10.

## Notes

# Using It

```
// Add the Dep
libraryDependencies += "keen" %% "keenclient-scala" % "0.0.1"

// And a the resolver
resolvers += "keenlabs" at "https://raw.github.com/keenlabs/mvn-repo/master/releases/",
```

## Environment Variables

You'll want to set the following environment variables:

* KEEN_PROJECT_ID
* KEEN_MASTER_KEY
* KEEN_WRITE_KEY
* KEEN_READ_KEY

## Example

```
import scala.concurrent.Await
import scala.concurrent.duration._
import keen._

val client = new Client()

// Verify the index exists
client.projects.getStatusCode // Should be 200!
```
