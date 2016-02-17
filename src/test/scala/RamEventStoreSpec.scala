package test

import io.keen.client.scala.RamEventStore

import org.specs2.mutable.Specification

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

class RamEventStoreSpec extends Specification {

  sequential

  "RamEventStore" should {

    "not exceed the maximum number of events per collection" in {
      
      val store: RamEventStore = new RamEventStore
      store.maxEventsPerCollection = 3

      // generate 5 test events and add them to the store and keep track of them so
      // we can use them later in the test
      val testEvents: ArrayBuffer[String] = new ArrayBuffer[String](5)
      for(i <- 0 to 4) {
        testEvents += s"""{"param$i":"value$i"}"""
        store.store("project1", "collection1", testEvents(i))
      }
      
      // get the handle map
      val handleMap: HashMap[String, ArrayBuffer[Any]] = store.getHandles("project1")
      handleMap must not beNull      

      // get the lists of handles
      val handles: ArrayBuffer[Any] = handleMap.getOrElse("collection1", null)
      (handles must not beNull)
      handles.size must beEqualTo(3)

      // get the events
      val retrievedEvents: ArrayBuffer[String] = new ArrayBuffer[String]()
      for(handle <- handles) {
        val retrievedEvent: String = store.get(handle)
        (retrievedEvent must not beNull)
        retrievedEvents += retrievedEvent
      }

      // validate that there are a total of 3 events in RamEventStore, the last 3
      // we added in fact, because the first 2 have already been purged from the store
      retrievedEvents must contain(allOf(testEvents(2), testEvents(3), testEvents(4)))
    }
    
  }
}