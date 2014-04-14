# KeenClient-Scala

**Note**: This library is currently in development and does not implement all of the features of the Keen API.
Single event publishing works.Other API features will be added over time. **The interface will almost
certainly change!**

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

You'll have to compile from source for now.

```
sbt package
```

You'll find a jar in `target/scala-2.10`. Something like `keenclient-scala_2.10-VERSION.jar`

# Testing It

This test suite includes integration tests which require keys and access to Keen IO's
API. You can skip them with

```
test-only * -- exclude integration
```

## Environment Variables

You'll want to set the following environment variables:

* KEEN_PROJECT_ID
* KEEN_MASTER_KEY
* KEEN_WRITE_KEY
* KEEN_READ_KEY

## Example

```scala
import io.keen.client.scala.Client

val client = new Client(
  projectId = sys.env("KEEN_PROJECT_ID"),
  masterKey = sys.env("KEEN_MASTER_KEY"),
  writeKey = sys.env("KEEN_WRITE_KEY"),
  readKey = sys.env("KEEN_READ_KEY")
)


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
