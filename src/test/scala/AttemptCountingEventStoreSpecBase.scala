package test

import io.keen.client.scala.AttemptCountingEventStore

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer

import org.specs2.mutable.BeforeAfter

abstract class AttemptCountingEventStoreSpecBase extends EventStoreSpecBase {

  var attemptCountingStore: AttemptCountingEventStore = _
  
  trait AttemptCountingEventStoreSetupTeardown extends BeforeAfter {

    def before: Any = {
      store = buildStore()  // initialize our store
      attemptCountingStore = store.asInstanceOf[AttemptCountingEventStore]
    }

    def after: Any = {}

  }

  sequential

  "AttemptCountingEventStore" should {

    "store and get event attempts" in new AttemptCountingEventStoreSetupTeardown {

      val attempts: String = "blargh"
      attemptCountingStore.setAttempts("project1", "collection1", attempts)
      attempts must beEqualTo(attemptCountingStore.getAttempts("project1", "collection1"))

    }
    
    "get handles with attempts" in new AttemptCountingEventStoreSetupTeardown {

      // add a couple events to the store
      attemptCountingStore.store("project1", "collection1", testEvents(0))
      attemptCountingStore.store("project1", "collection2", testEvents(1))

      // set some value for attempts.json. this is to ensure that setting attempts doesn't
      // interfere with getting handles
      attemptCountingStore.setAttempts("project1", "collection1", "{}");

      // get the handle map
      val handleMap: TrieMap[String, ListBuffer[Long]] = attemptCountingStore.getHandles("project1")
      (handleMap must not beNull)
      handleMap.size must beEqualTo(2)

      // get the lists of handles
      var handles1: ListBuffer[Long] = handleMap.getOrElse("collection1", null)
      (handles1 must not beNull)
      handles1.size must beEqualTo(1)
      var handles2: ListBuffer[Long] = handleMap.getOrElse("collection2", null)
      (handles2 must not beNull)
      handles2.size must beEqualTo(1)

      // validate the actual events
      store.get(handles1(0)) must beEqualTo(testEvents(0))
      store.get(handles2(0)) must beEqualTo(testEvents(1))

    }

  }
}