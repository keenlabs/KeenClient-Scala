package test

import io.keen.client.scala.EventStore
import io.keen.client.scala.RamEventStore

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer

import org.specs2.mutable.Specification

class RamEventStoreSpec extends AttemptCountingEventStoreSpecBase {

  override def buildStore(): EventStore = {
    new RamEventStore
  }

  sequential

  "RamEventStore" should {

    "not exceed the maximum number of events per collection" in {

      val ramStore: EventStore = store.asInstanceOf[RamEventStore]
      ramStore.maxEventsPerCollection = 3

      // add 5 events
      for(i <- 0 to 4) {
        store.store("project1", "collection1", testEvents(i))
      }
      
      // get the handle map
      val handleMap: TrieMap[String, ListBuffer[Long]] = ramStore.getHandles("project1")
      (handleMap must not beNull)

      // get the lists of handles
      val handles: ListBuffer[Long] = handleMap.getOrElse("collection1", null)
      (handles must not beNull)
      handles.size must beEqualTo(3)

      // get the events
      val retrievedEvents: ListBuffer[String] = new ListBuffer[String]()
      for(handle <- handles) {
        val retrievedEvent: String = ramStore.get(handle)
        (retrievedEvent must not beNull)
        retrievedEvents += retrievedEvent
      }

      // validate that there are a total of 3 events in RamEventStore, the last 3
      // we added in fact, because the first 2 have already been purged from the store
      retrievedEvents must contain(allOf(testEvents(2), testEvents(3), testEvents(4)))

    }
    
  }
}