package test

import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future, TimeoutException }
import scala.util.Try

import akka.actor.ActorSystem
import akka.pattern.AskTimeoutException
import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import spray.http.Uri
import spray.http.Uri._

import io.keen.client.scala._

class ClientSpec extends Specification with NoTimeConversions {
  // Timeout used for most future awaits, etc. With unit tests/mocking this
  // shouldn't normally need to be long.
  val timeout = 1.second

  class OkHttpAdapter extends HttpAdapter {

    var lastUrl: Option[String] = None
    var lastKey: Option[String] = None

    override def doRequest(
      scheme: String,
      authority: String,
      path: String,
      method: String,
      key: String,
      body: Option[String] = None,
      params: Map[String, Option[String]] = Map.empty
    ): Future[Response] = {

      // We have a map of str,opt[str] and we need to convert it to
      val filteredParams = params.filter(
        // Filter out keys that are None
        _._2.isDefined
      ).map(
        // Convert the remaining tuples to str,str
        param => (param._1 -> param._2.get)
      )
      // Make a Uri
      val finalUrl = Uri(
        scheme = scheme,
        authority = Authority(host = Host(authority)),
        path = Path("/" + path),
        query = Query(filteredParams)
      )

      lastUrl = Some(finalUrl.toString)
      lastKey = Some(key)
      Future { Response(200, "Ok") }
    }

    def getKey = lastKey

    def getUrl = lastUrl

    def shutdown() = {}
  }

  class FiveHundredHttpAdapter extends HttpAdapterSpray {

    override def doRequest(
      scheme: String,
      authority: String,
      path: String,
      method: String,
      key: String,
      body: Option[String] = None,
      params: Map[String, Option[String]] = Map.empty
    ): Future[Response] = {
      Future {
        Response(500, "Internal Server Error")
      }
    }
  }

  class SlowHttpAdapter extends HttpAdapterSpray {

    override def doRequest(
      scheme: String,
      authority: String,
      path: String,
      method: String,
      key: String,
      body: Option[String] = None,
      params: Map[String, Option[String]] = Map.empty
    ): Future[Response] = {
      Future.failed(new AskTimeoutException("I timed out!"))
    }
  }

  val dummyConfig = ConfigFactory.parseMap(
    Map(
      "keen.project-id" -> "abc",
      "keen.optional.master-key" -> "masterKey",
      "keen.optional.read-key" -> "readKey",
      "keen.optional.write-key" -> "writeKey"
    )
  )

  // generates n test events
  def generateTestEvents(n: Integer): ListBuffer[String] = {
    val events: ListBuffer[String] = new ListBuffer[String]
    for (i <- 1 to n) {
      events += s"""{"param$i":"value$i"}"""
    }
    events
  }

  // Sequential because it's less work to share the client instance
  // TODO: set up separate read-only client, writer client, etc. instead of
  // doing everything in this suite with a master key.
  sequential

  "Client" should {

    val client = new Client(config = dummyConfig) with Master {
      override val httpAdapter = new OkHttpAdapter
    }

    val adapter = client.httpAdapter
    val projectId: String = dummyConfig.getString("keen.project-id")

    "handle 200" in {
      val res = Await.result(client.getProjects, timeout)

      res.statusCode must beEqualTo(200)
    }

    "handle get projects" in {
      val res = Await.result(client.getProjects, timeout)

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get project" in {
      val res = Await.result(client.getProject, timeout)

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get event" in {
      val res = Await.result(client.getEvents, timeout)

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/events")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get property" in {
      val res = Await.result(client.getProperty("foo", "bar"), timeout)

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo/properties/bar")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get collection" in {
      val res = Await.result(client.getCollection("foo"), timeout)

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get collection (encoding)" in {
      val res = Await.result(client.getCollection("foo foo"), timeout)

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo%20foo")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle count query" in {
      val res = Await.result(client.count("foo"), timeout)

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/queries/count?event_collection=foo")

      adapter.getKey.get must beEqualTo("masterKey")
    }

    // We'll test average thoroughly but since all the query method use the same underlying
    // implementation to build URLs we can skip that on the next ones. We'll get nasty in
    // here with encodings and such.
    "handle average query" in {
      val res = Await.result(client.average(
        collection = "foo foo❖",
        targetProperty = "bar",
        filters = Some("""[{"property_name": "baz","operator":"eq","property_value":"gorch"}]"""),
        timeframe = Some("this_week"),
        timezone = Some("America/Chicago"),
        groupBy = Some("foo.name")
      ), timeout)

      res.statusCode must beEqualTo(200)
      var url = adapter.getUrl.get

      // Test for these with contains because who knows what order they'll be in
      url must contain("https://api.keen.io/3.0/projects/abc/queries/average?")
      url must contain("event_collection=foo+foo%E2%9D%96")
      url must contain("target_property=bar")
      url must contain("filters=%5B%7B%22property_name%22:+%22baz%22,%22operator%22:%22eq%22,%22property_value%22:%22gorch%22%7D%5D")
      url must contain("timeframe=this_week")
      url must contain("timezone=America/Chicago")
      url must contain("group_by=foo.name")

      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle extraction query" in {
      val res = Await.result(client.extraction(
        collection = "foo",
        filters = Some("""[{"property_name": "baz","operator":"eq","property_value":"gorch"}]"""),
        timeframe = Some("this_week"),
        email = Some("test@example.com"),
        latest = Some("1"),
        propertyNames = Some("""["abc","def"]""")
      ), timeout)

      res.statusCode must beEqualTo(200)
      var url = adapter.getUrl.get

      url must contain("https://api.keen.io/3.0/projects/abc/queries/extraction?")
      url must contain("event_collection=foo")
      url must contain("filters=%5B%7B%22property_name%22:+%22baz%22,%22operator%22:%22eq%22,%22property_value%22:%22gorch%22%7D%5D")
      url must contain("timeframe=this_week")
      url must contain("email=test@example.com")
      url must contain("latest=1")
      url must contain("%5B%22abc%22,%22def%22%5D")
    }

    "shutdown" in {
      client.shutdown()
      1 must beEqualTo(1)
    }
  }

  "Client with Reader" >> {
    class ReadClient(config: Config) extends Client(config) with Reader

    "without a read key configured" >> {
      val badConfig = dummyConfig.withoutPath("keen.optional.read-key")

      "throws MissingCredential on construction" in {
        new ReadClient(config = badConfig) must throwA[MissingCredential]
      }
    }
  }

  "Client with Writer" >> {
    class WriteClient(config: Config) extends Client(config) with Writer

    "without a write key configured" >> {
      val badConfig = dummyConfig.withoutPath("keen.optional.write-key")

      "throws MissingCredential on construction" in {
        new WriteClient(config = badConfig) must throwA[MissingCredential]
      }
    }
  }

  "Client with Master" >> {
    class MasterClient(config: Config) extends Client(config) with Master

    "without a master key configured" >> {
      val badConfig = dummyConfig.withoutPath("keen.optional.master-key")

      "throws MissingCredential on construction" in {
        new MasterClient(config = badConfig) must throwA[MissingCredential]
      }
    }
  }

  "Client with interval based queueing enabled" should {

    val queueConfig = ConfigFactory.parseMap(
      Map(
        "keen.project-id" -> "abc",
        "keen.optional.environment" -> "test",
        "keen.optional.master-key" -> "masterKey",
        "keen.optional.read-key" -> "readKey",
        "keen.optional.write-key" -> "writeKey",
        "keen.optional.queue.batch.size" -> 5,
        "keen.optional.queue.batch.timeout" -> 5,
        "keen.optional.queue.max-events-per-collection" -> 250,
        "keen.optional.queue.send-interval.events" -> 100,
        "keen.optional.queue.send-interval.seconds" -> 5,
        "keen.optional.queue.shutdown-delay" -> 0
      )
    )

    val client = new Client(config = queueConfig) with Master {
      override val httpAdapter = new OkHttpAdapter
    }

    val collection: String = "foo"
    val projectId: String = dummyConfig.getString("keen.project-id")

    var handleMap: TrieMap[String, ListBuffer[Long]] = new TrieMap[String, ListBuffer[Long]]()
    val store: EventStore = client.eventStore.asInstanceOf[RamEventStore]
    var testEvents: ListBuffer[String] = new ListBuffer[String]()

    "send queued events" in {

      testEvents = generateTestEvents(5)
      testEvents.foreach { event => client.queueEvent(collection, event) }

      // verify that the expected number of events are in the store
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(5)

      // send the queued events
      client.sendQueuedEvents()

      // validate that the store is now empty
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(0)

      // try sending events again, nothing should happen because the queue is empty
      client.sendQueuedEvents()

      // the store should still be empty
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(0)

    }

    "automatically send queued events when queue reaches keen.optional.queue.send-interval.events" in {

      testEvents = generateTestEvents(100)

      // queue the first 50 events
      for (i <- 0 to 49) {
        client.queueEvent(collection, testEvents(i))
      }

      // verify that the expected number of events are in the store
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(50)

      // add the final 50 events
      for (i <- 50 to 99) {
        client.queueEvent(collection, testEvents(i))
      }

      // validate that the store is now empty as a result of sendQueuedEvents being automatically 
      // triggered with the queueing of the 100th event
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(0)

    }

    "automatically send queued events every keen.optional.queue.send-interval.seconds" in {

      testEvents = generateTestEvents(5)
      testEvents.foreach { event => client.queueEvent(collection, event) }

      // verify that the expected number of events are in the store
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(5)

      // sleep until the set interval is reached
      Thread.sleep((queueConfig.getInt("keen.optional.queue.send-interval.seconds") + 1) * 1000)

      // validate that the store is now empty as a result of sendQueuedEvents being automatically 
      // triggered with the queueing of the 100th event
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(0)

    }

    "send queued events on shutdown" in {

      testEvents = generateTestEvents(5)
      testEvents.foreach { event => client.queueEvent(collection, event) }

      // verify that the expected number of events are in the store
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(5)

      // send the queued events
      client.shutdown()

      // validate that the store is now empty
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(0)

    }

  }

  "Client with simple queueing enabled" should {

    val queueConfig = ConfigFactory.parseMap(
      Map(
        "keen.project-id" -> "abc",
        "keen.optional.environment" -> "test",
        "keen.optional.master-key" -> "masterKey",
        "keen.optional.read-key" -> "readKey",
        "keen.optional.write-key" -> "writeKey",
        "keen.optional.queue.batch.size" -> 5,
        "keen.optional.queue.batch.timeout" -> 5,
        "keen.optional.queue.max-events-per-collection" -> 250,
        "keen.optional.queue.send-interval.events" -> 0,
        "keen.optional.queue.send-interval.seconds" -> 0,
        "keen.optional.queue.shutdown-delay" -> 0
      )
    )

    val client = new Client(config = queueConfig) with Master {
      override val httpAdapter = new OkHttpAdapter
    }

    val collection: String = "foo"
    val projectId: String = dummyConfig.getString("keen.project-id")

    var handleMap: TrieMap[String, ListBuffer[Long]] = new TrieMap[String, ListBuffer[Long]]()
    val store: EventStore = client.eventStore.asInstanceOf[RamEventStore]
    var testEvents: ListBuffer[String] = new ListBuffer[String]()

    "not exceed keen.optional.queue.max-events-per-collection" in {

      testEvents = generateTestEvents(500)
      testEvents.foreach { event => client.queueEvent(collection, event) }

      // verify that the expected number of events are in the store
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(queueConfig.getInt("keen.optional.queue.max-events-per-collection"))

      // shutdown the client
      client.shutdown()

      // validate that the store is now empty
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(0)

    }

  }

  "Client with Spray HttpAdapter" should {
    lazy val externalSystem = ActorSystem("keen-test-user-supplied")

    "use explicit user-supplied actor system" in {
      val adapter = new HttpAdapterSpray()(externalSystem)
      val client = new Client(config = dummyConfig) {
        override val httpAdapter = adapter
      }
      adapter.actorSystem must be(externalSystem)

      // We don't terminate a user-supplied actor system
      client.shutdown()
      externalSystem.awaitTermination(timeout) must throwA[TimeoutException]
      externalSystem.isTerminated must beFalse
    }

    "use implicit user-supplied actor system" in {
      implicit val system = externalSystem
      val client = new Client(config = dummyConfig) {
        override val httpAdapter = new HttpAdapterSpray
      }
      // TODO: httpAdapter field should be private
      client.httpAdapter.actorSystem must be(externalSystem)

      client.shutdown()
      externalSystem.awaitTermination(timeout) must throwA[TimeoutException]
      externalSystem.isTerminated must beFalse
    }

    step {
      externalSystem.shutdown()
    }
  }

  "Client 500 failures" should {

    val client = new Client(config = dummyConfig) with Master {
      override val httpAdapter = new FiveHundredHttpAdapter()
    }

    "handle 500" in {
      val res = Await.result(client.getProjects, timeout)

      res.statusCode must beEqualTo(500)
    }

    "send queued events with server failure" in {

      val projectId: String = dummyConfig.getString("keen.project-id")
      val collection: String = "foo"
      val testEvents: ListBuffer[String] = generateTestEvents(5)

      // queue the events
      for (event <- testEvents) {
        client.queueEvent(collection, event)
      }

      // verify that the expected number of events are in the store
      val store: EventStore = client.eventStore.asInstanceOf[RamEventStore]
      var handleMap: TrieMap[String, ListBuffer[Long]] = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(5)

      // send the queued events
      client.sendQueuedEvents()

      // validate that the store still contains all of the queued events
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(5)

      // shutdown the client
      client.shutdown()
      true must beEqualTo(true)
    }

  }

  "Client future failures" should {

    val client = new Client(config = dummyConfig) with Master {
      override val httpAdapter = new SlowHttpAdapter
    }

    "handle timeout" in {
      Await.result(client.getProjects, timeout) must throwA[AskTimeoutException]
    }
  }

  "Client with Dispatch HttpAdapter" should {

    "handle dispatch without an actor system" in {
      val attempt = Try({
        val client = new Client(config = dummyConfig) {
          override val httpAdapter = new HttpAdapterDispatch
        }
      })
      attempt must beSuccessfulTry
    }
  }
}

