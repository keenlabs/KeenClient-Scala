# KeenClient-Scala

[![Build Status]](https://travis-ci.org/keenlabs/KeenClient-Scala)

The official asynchronous Scala client for the [Keen IO] API.

**Note**: This library is in early development and does not implement all of the
features of the Keen API. It is pre-1.0 in the [Semantic Versioning] sense:
public interfaces may change without backwards compatibility. We will try to
minimize breaking changes, but please consult [the changelog] when updating to a
new release version.

Additional API features will be added over time. Contributions are welcome!

## Use It - A Quick Taste

```scala
import io.keen.client.scala.{ Client, Writer }

// Assumes you've configured a write key as explained in Configuration below
val keen = new Client with Writer

// Publish an event!
keen.addEvent(
  collection = "collectionNameHere",
  event = """{"foo": "bar"}"""
)

// Publish lots of events!
keen.addEvents(someEvents)

// Responses are Futures - handle errors!
val resp = keen.addEvent(
  collection = "collectionNameHere",
  event = """{"foo": "bar"}"""
)

resp onComplete {
  case Success(r) => println(resp.statusCode)
  case Failure(t) => println(t.getMessage) // A Throwable
}

// Or using map
resp map { println("I succeeded!") } getOrElse { println("I failed :(") }

// You can even generate a scoped key!
val masterKeen = new Client with Master

val scopedKey = masterKeen.getScopedKey(List("read"))
val narrowerScopedKey = masterKeen.getScopedKey(List("read"), Some("""[{
    "property_name": "user_id",
    "operator": "eq",
    "property_value": 123
}]"""))
```

## Get It

Artifacts for keen-client-scala are [hosted on Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ckeenclient-scala).
You can use them in your project with SBT thusly:

```scala
libraryDependencies += "io.keen" %% "keenclient-scala" % "0.5.0"
```

Note that we publish artifacts for Scala 2.10 and 2.11, so you can either use `%%` to automatically pick the correct
version or specify them explicitly with something like:

```scala
libraryDependencies += "io.keen" % "keenclient-scala_2.10" % "0.5.0"
```

## Configuration

The client has a notion of access levels that reflect [the Keen IO API key
security model][security]. These are represented by Scala traits called
`Reader`, `Writer`, and `Master`. According to the level of access that your
application requires, you must mix the appropriate trait(s) into your client
instance when creating it, and configure your corresponding API keys. This is
demonstrated in the examples.

Configuration is supported by the [Typesafe config] library, offering all the
flexibility you could wish to provide settings through a file, environment
variables, or programmatically. We recommend environment variables for your API
keys at very least, to avoid storing credentials in source control. To that end,
the following will be honored by default if set:

* `KEEN_PROJECT_ID`
* `KEEN_READ_KEY`
* `KEEN_WRITE_KEY`
* `KEEN_MASTER_KEY`

To configure with a file, it must be on the classpath, customarily called
`application.conf` though you may use others--see the Typesafe config
documentation for all the options and details of the file format. [Our
`reference.conf`] reflects all of the settings you can configure and their
default values.

For advanced needs, you may provide your own custom [`Config`] object by simply
passing it to the client constructor:

```scala
import io.keen.client.scala.Client

val keen = new Client(config = myCustomConfigObject)
```

When using environment variables, you might like [sbt-dotenv] in your
development setup (install it as a [global plugin], and `chmod 600` your `.env`
files that contain credentials!). In production, a [good service manager][runit]
can set env vars for app processes with ease. On Heroku you'll be right at home.

## Dependencies

The client's default HTTP adapter is built on the [spray HTTP toolkit][spray],
which is [Akka]-based and asynchronous. A [Dispatch]-based adapter is also
available. At this time spray (and thus Akka) is a hard dependency, but if there
is demand we may consider designating it as "provided" so that you may opt for
the Dispatch adapter (or a custom one for your preferred HTTP client) and avoid
pulling in Akka dependencies if you wish, or to avoid version conflicts. Please
share your feedback if you find the spray deps burdensome.

With either adapter, API calls will return a uniform `Future[Response]` type,
where `Response` is an `io.keen.client.scala.Response`. Instances have
`statusCode` and `body` attributes that you may inspect to act on errors. An
example of choosing the Dispatch adapter is shown below.

The client also depends on [grizzled-slf4j] for logging.

It is cross-compiled for 2.10 and 2.11 Scala versions. If you are interested in
support for other versions or discover any binary compatibility problems, please
share your feedback.

### Using the Dispatch adapter

To use the Dispatch HTTP adapter instead of the Spray default, specify the
following override when instantiating a `Client` instance:

```scala
import io.keen.client.scala.{ Client, HttpAdapterDispatch }

val keen = new Client {
  override val httpAdapter = new HttpAdapterDispatch
}
```

### JSON

Presently this library does **not** do any JSON parsing. It works with strings only. It is
assumed that you will parse the JSON returned and pass stringified JSON into any methods that
require it. Feedback is welcome!

We understand that Scala users value the language's strong type system. Again,
we wish to avoid unwanted dependencies given that there are so many JSON parsing
libraries out there. We'd eventually like to offer rich types through JSON
adapters with optional deps.

## Hack On It

Unit tests can be run with the standard SBT commands `test`, `testQuick`, etc.

The test suite includes integration tests which require keys and access to Keen
IO's API. If you have set keys through environment variables or configuration as
described above, you may run these with:

    $ sbt it:test

**Only use a dedicated dummy account for this purpose, data could be destroyed
that you didn't expect!**


[Build Status]: https://travis-ci.org/keenlabs/KeenClient-Scala.svg?branch=master
[Keen IO]: http://keen.io/
[Semantic Versioning]: http://semver.org/
[the changelog]: https://github.com/keenlabs/KeenClient-Scala/blob/master/CHANGELOG
[spray]: http://spray.io
[Akka]: http://akka.io
[Dispatch]: http://dispatch.databinder.net/
[grizzled-slf4j]: http://software.clapper.org/grizzled-slf4j/
[security]: https://keen.io/docs/security/
[Typesafe config]: https://github.com/typesafehub/config
[Our `reference.conf`]: https://github.com/keenlabs/KeenClient-Scala/tree/master/src/main/resources/reference.conf
[`Config`]: http://typesafehub.github.io/config/latest/api/com/typesafe/config/Config.html
[sbt-dotenv]: https://github.com/mefellows/sbt-dotenv
[global plugin]: http://www.scala-sbt.org/0.13/docs/Global-Settings.html
[runit]: http://smarden.org/runit/
