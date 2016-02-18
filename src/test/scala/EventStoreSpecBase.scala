package test

import io.keen.client.scala.KeenEventStore

import java.io.IOException

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.specification.{Step, Fragments}

trait BeforeAllAfterAll extends Specification {

  override def map(fragments: => Fragments) =
    Step(beforeAll) ^ fragments ^ Step(afterAll)

  protected def beforeAll()
  protected def afterAll()

}

abstract class EventStoreSpecBase extends Specification with BeforeAllAfterAll {

  @throws(classOf[IOException])
  def buildStore(): KeenEventStore

  var store: KeenEventStore = _
  val testEvents: ListBuffer[String] = new ListBuffer[String]

  def beforeAll() {
    store = buildStore()  // initialize our store

    // generate 5 test events and add them to the store and keep track of them so
    // we can use them later in the test
    for(i <- 0 to 4) {
      testEvents += s"""{"param$i":"value$i"}"""
    }
  }

  def afterAll() {
    store = null  // cleanup
  }

  trait EventStoreSetupTeardown extends BeforeAfter {

    def before: Any = {
      store = buildStore()  // initialize our store
    }

    def after: Any = {}

  }

  sequential

  "EventStoreSpecBase" should {

    "store and get event" in new EventStoreSetupTeardown {

      val handle: Long = store.store("project1", "collection1", testEvents(0))
      val retrieved: String = store.get(handle)
      retrieved must beEqualTo(testEvents(0))

    }

    "remove event" in new EventStoreSetupTeardown {

      val handle: Long = store.store("project1", "collection1", testEvents(0))
      store.remove(handle)
      val retrieved: String = store.get(handle)
      (retrieved must beNull)

    }

    "remove handle twice" in new EventStoreSetupTeardown {

      val handle: Long = store.store("project1", "collection1", testEvents(0))
      store.remove(handle)
      store.remove(handle)
      true must beEqualTo(true) // we're testing for an exception here, and if none is thrown, pass

    }

    "get handles" in new EventStoreSetupTeardown {

      // add a couple events to the store
      store.store("project1", "collection1", testEvents(0))
      store.store("project1", "collection2", testEvents(1))

      // get the handle map
      val handleMap: HashMap[String, ListBuffer[Long]] = store.getHandles("project1")
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

    "get handles with no events" in new EventStoreSetupTeardown {

      val handleMap: HashMap[String, ListBuffer[Long]] = store.getHandles("project1")
      (handleMap must not beNull)
      handleMap.size must beEqualTo(0)

    }

    "get handles for multiple projects" in new EventStoreSetupTeardown {

      // add a couple events to the store in different projects
      store.store("project1", "collection1", testEvents(0))
      store.store("project1", "collection2", testEvents(1))
      store.store("project2", "collection3", testEvents(2))
      store.store("project2", "collection3", testEvents(3))

      // get and validate the handle map for project 1
      var handleMap: HashMap[String, ListBuffer[Long]] = store.getHandles("project1")
      (handleMap must not beNull)
      handleMap.size must beEqualTo(2)
      handleMap.getOrElse("collection1", null).size must beEqualTo(1)
      handleMap.getOrElse("collection2", null).size must beEqualTo(1)

      // get and validate the handle map for project 2
      handleMap = store.getHandles("project2")
      (handleMap must not beNull)
      handleMap.size must beEqualTo(1)
      handleMap.getOrElse("collection3", null).size must beEqualTo(2)

    }

  }

}