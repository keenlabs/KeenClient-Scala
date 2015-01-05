# KeenClient-Scala

**Note**: This library is currently in development and does not implement all of the features of the Keen API.
Single event publishing works. Other API features will be added over time.

keen-scala uses the [spray-can](http://spray.io/) HTTP library.
It's all async so all of the returned values are
`Future[Response]`.

The returned object is an [`io.keen.client.scala.Response`](src/main/scala/io/keen/client/scala/package.scala). You can look at it's
`statusCode` or `body` attributes to verify something didn't go awry.

## JSON

Presently this library does **not** do any JSON parsing. It works with strings only. It is
assumed that you will parse the JSON returned and pass stringified JSON into any methods that
require it. Feedback is welcome!

## Dependencies

Depends on [spray](http://spray.io/) and
[grizzled-slf4j](http://software.clapper.org/grizzled-slf4j/). It cross-compiles for versions of scala 2.10.

# Using It

Artifacts for keen-client-scala are [hosted on Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ckeenclient-scala).
You can use them in your project with SBT thusly:

```scala
libraryDependencies += "io.keen" %% "keenclient-scala" % "0.4.0"
```

Note that we publish artifacts for Scala 2.10 and 2.11, so you can either use `%%` to automatically pick the correct
version or specify them explicitly with something like:

```scala
libraryDependencies += "io.keen" % "keenclient-scala_2.10" % "0.4.0"
```

# Testing It

This test suite includes integration tests which require keys and access to Keen IO's
API. You can skip them with

```
test-only * -- exclude integration
```

# Configuration

The client has a notion of access levels that reflect [the Keen IO API key
security model][security]. These are represented by Scala traits called
`Reader`, `Writer`, and `Master`. According to the level of access that your
application requires, you must mix the appropriate trait(s) into your client
instance when creating it, and configure your corresponding API keys. This is
demonstrated in the example below.

Our recommended means of providing settings is through environment variables, to
avoid storing credentials in source control. We intend to support a config file
soon, but would still discourage you from using that for credentials.

## Example

```scala
import io.keen.client.scala.{ Client, Reader, Writer }

// You probably have some form of configuration object in your app already,
// this is just an example.
object KeenSettings {
  val projectId = sys.env("KEEN_PROJECT_ID")
  val readKey = sys.env("KEEN_READ_KEY")
  val writeKey = sys.env("KEEN_WRITE_KEY")
}

// Construct a client with read and write access, providing the required keys.
val client = new Client(KeenSettings.projectId) with Reader with Writer {
  override val readKey = KeenSettings.readKey
  override val writeKey = KeenSettings.writeKey
}


// Publish an event!
client.addEvent(
  collection = "collectionNameHere",
  event = """{"foo": "bar"}"""
)

// Publish an event and care about the result!
val resp = client.addEvent(
  collection = "collectionNameHere",
  event = """{"foo": "bar"}"""
)

// Publish lots of events
client.addEvents(someEvents)

// Add an onComplete callback for failures!
resp onComplete {
  case Success(r) => println(resp.statusCode)
  case Failure(t) => println(t.getMessage) // A Throwable
}

// Or use a map
resp map {
  println("I succeeded!")
} getOrElse {
  println("I failed :(")
}
```

[security]: https://keen.io/docs/security/

