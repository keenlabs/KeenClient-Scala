package io.keen.client.scala
package test

import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory

// TODO: Factor the base Client specs so that they can all be included here, with
// an injected BatchWriterClient
class BatchWriterClientSpec extends ClientSpecification {
  // generates n test events
  def generateTestEvents(n: Integer): ListBuffer[String] = {
    val events = ListBuffer.empty[String]
    for (i <- 1 to n) {
      events += s"""{"param$i":"value$i"}"""
    }
    events
  }

  // TODO: This suite should clear the eventStore between tests, not do this
  sequential

  "BatchWriterClient with interval-based queueing" should {
    val queueConfig = ConfigFactory.parseMap(
      Map(
        "keen.optional.environment" -> "test",
        "keen.queue.batch.size" -> 5,
        "keen.queue.batch.timeout" -> "5 seconds",
        "keen.queue.max-events-per-collection" -> 250,
        "keen.queue.send-interval.events" -> 100,
        "keen.queue.send-interval.duration" -> "2 seconds",
        "keen.queue.shutdown-delay" -> "0s"
      )
    ).withFallback(dummyConfig)

    val client = new BatchWriterClient(config = queueConfig) {
      override val httpAdapter = new OkHttpAdapter
    }

    val collection = "foo"
    val projectId = client.settings.projectId

    var handleMap = TrieMap.empty[String, ListBuffer[Long]]
    val store = client.eventStore
    var testEvents = ListBuffer.empty[String]

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

    "automatically send queued events when queue reaches keen.queue.send-interval.events" in {
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

    "automatically send queued events every keen.queue.send-interval.duration" in {
      testEvents = generateTestEvents(5)
      testEvents.foreach { event => client.queueEvent(collection, event) }

      // verify that the expected number of events are in the store
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(5)

      // sleep until the set interval is reached
      // FIXME: This is basically an integration test, and slow. We could test this
      // with a mock that verifies sendQueuedEvents is called after shorter duration.
      // It's brittle too, use test framework time dilation features.
      Thread.sleep((client.settings.sendIntervalDuration + 100.millis).toMillis)

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

  "BatchWriterClient with simple queueing" should {
    val queueConfig = ConfigFactory.parseMap(
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

    val client = new BatchWriterClient(config = queueConfig) {
      override val httpAdapter = new OkHttpAdapter
    }

    val collection = "foo"
    val projectId = client.settings.projectId

    var handleMap = TrieMap.empty[String, ListBuffer[Long]]
    val store = client.eventStore
    var testEvents = ListBuffer.empty[String]

    "not exceed keen.queue.max-events-per-collection" in {
      testEvents = generateTestEvents(500)
      testEvents.foreach { event => client.queueEvent(collection, event) }

      // verify that the expected number of events are in the store
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse(collection, null).size must beEqualTo(store.maxEventsPerCollection)

      // shutdown the client
      client.shutdown()

      // validate that the store is now empty
      handleMap = store.getHandles(projectId)
      handleMap.size must beEqualTo(0)
    }
  }

  "BatchWriterClient on 500 errors" should {
    val client = new BatchWriterClient(config = dummyConfig) {
      override val httpAdapter = new FiveHundredHttpAdapter()
    }

    "send queued events with server failure" in {
      val projectId = client.settings.projectId
      val collection = "foo"
      val testEvents = generateTestEvents(5)

      // queue the events
      for (event <- testEvents) {
        client.queueEvent(collection, event)
      }

      // verify that the expected number of events are in the store
      val store = client.eventStore
      var handleMap = store.getHandles(projectId)
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
}
