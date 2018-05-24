package io.keen.client.scala
package test

import com.typesafe.config.{Config, ConfigFactory}
import org.specs2.execute.Skipped
import org.specs2.matcher.{EventuallyMatchers, MatchResult}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

// TODO: Factor the base Client specs so that they can all be included here, with
// an injected BatchWriterClient
class BatchWriterClientSpec extends ClientSpecification with EventuallyMatchers {
  // Examples each run in a fresh class instance, with its own copies of the
  // below mutable test vars, a new client & store, etc.
  isolated

  val collection = "foo"
  var queueConfig: Config = _
  var testEvents: ListBuffer[String] = _

  // Late-bind the client for varying queueConfig
  lazy val client = new BatchWriterClient(config = queueConfig) {
    override val httpAdapter = new OkHttpAdapter
  }

  lazy val projectId = client.settings.projectId
  lazy val store = client.eventStore

  def queueForTestCollection(event: String) = client.queueEvent(collection, event)

  // generates n test events
  def generateTestEvents(n: Integer): ListBuffer[String] = {
    val events = ListBuffer.empty[String]
    for (i <- 1 to n) {
      events += s"""{"param$i":"value$i"}"""
    }
    events
  }

  def assertEventualStoreHandlesSize(size: Int): MatchResult[Int] = {
    store.getHandles(projectId).size must beEqualTo(size).eventually(10, 1000.millis)
  }

  def assertEventualStoreHandlesCollectionSize(size: Int): MatchResult[Int] = {
    size must beGreaterThan(0)
    store.getHandles(projectId).contains(collection) must beTrue.eventually(10, 1000.millis)
    store.getHandles(projectId)(collection).size must beEqualTo(size).eventually(10, 1000.millis)
  }

  "BatchWriterClient with interval-based queueing" should {
    queueConfig = ConfigFactory.parseMap(
      Map(
        "keen.optional.environment" -> "test",
        "keen.queue.batch.size" -> 5,
        "keen.queue.batch.timeout" -> "5 seconds",
        "keen.queue.max-events-per-collection" -> 250,
        "keen.queue.send-interval.events" -> 100,
        "keen.queue.send-interval.duration" -> "3600 seconds",
        "keen.queue.shutdown-delay" -> "0s"
      )
    ).withFallback(dummyConfig)


    "send queued events" in {
      skipped("Requires refactoring because of random failures: https://github.com/keenlabs/KeenClient-Scala/issues/51")

      testEvents = generateTestEvents(5)
      testEvents foreach queueForTestCollection

      // verify that the expected number of events are in the store
      assertEventualStoreHandlesSize(1)
      assertEventualStoreHandlesCollectionSize(5)

      // send the queued events
      client.sendQueuedEvents()

      // validate that the store is now empty
      assertEventualStoreHandlesSize(0)

      // try sending events again, nothing should happen because the queue is empty
      client.sendQueuedEvents()

      // the store should still be empty
      assertEventualStoreHandlesSize(0)
    }

    "send queued events asynchronously when queue exceeds its send-interval.events limit" in {
      skipped("Requires refactoring because of random failures: https://github.com/keenlabs/KeenClient-Scala/issues/51")

      testEvents = generateTestEvents(queueConfig.getInt("keen.queue.send-interval.events"))
      testEvents foreach queueForTestCollection

      // validate that the store is now empty
      assertEventualStoreHandlesSize(0)
    }

    "send queued events on shutdown" in {
      skipped("Requires refactoring because of random failures: https://github.com/keenlabs/KeenClient-Scala/issues/51")

      testEvents = generateTestEvents(5)
      testEvents foreach queueForTestCollection

      // verify that the expected number of events are in the store
      assertEventualStoreHandlesSize(1)
      assertEventualStoreHandlesCollectionSize(5)

      // send the queued events
      client.shutdown()

      // validate that the store is now empty
      assertEventualStoreHandlesSize(0)
    }
  }

  "BatchWriterClient with simple queueing" should {
    queueConfig = ConfigFactory.parseMap(
      Map(
        "keen.optional.environment" -> "test",
        "keen.queue.batch.size" -> 5,
        "keen.queue.batch.timeout" -> "5 seconds",
        "keen.queue.max-events-per-collection" -> 250,
        "keen.queue.send-interval.events" -> 0,
        "keen.queue.send-interval.duration" -> "0s",
        "keen.queue.shutdown-delay" -> "0s"
      )
    ).withFallback(dummyConfig)

    "not exceed keen.queue.max-events-per-collection" in {
      testEvents = generateTestEvents(500)
      testEvents foreach queueForTestCollection

      // verify that the expected number of events are in the store
      assertEventualStoreHandlesSize(1)
      assertEventualStoreHandlesCollectionSize(store.maxEventsPerCollection)

      // shutdown the client
      client.shutdown()

      // validate that the store is now empty
      assertEventualStoreHandlesSize(0)
    }
  }

  "BatchWriterClient on 500 errors" should {
    val client = new BatchWriterClient(config = dummyConfig) {
      override val httpAdapter = new FiveHundredHttpAdapter()
    }

    "send queued events with server failure" in {
      testEvents = generateTestEvents(5)
      testEvents foreach queueForTestCollection

      // verify that the expected number of events are in the store
      assertEventualStoreHandlesSize(1)
      assertEventualStoreHandlesCollectionSize(5)

      // send the queued events
      client.sendQueuedEvents()

      // validate that the store still contains all of the queued events
      assertEventualStoreHandlesSize(1)
      assertEventualStoreHandlesCollectionSize(5)

      // shutdown the client
      client.shutdown() must not(throwA[Exception])
    }
  }
}
