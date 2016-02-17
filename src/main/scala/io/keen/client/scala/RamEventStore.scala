package io.keen.client.scala

import java.io.IOException
import java.util.Locale

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.util.control.Breaks._

class RamEventStore extends KeenAttemptCountingEventStore {
  var maxEventsPerCollection: Integer = 10000

  @throws(classOf[IOException])
  override def store(projectId: String,
    eventCollection: String,
    event: String): Any = synchronized {

      // create a key from the project ID and event collection
      val key = "%s$%s".formatLocal(Locale.US, projectId, eventCollection)

      // get the list of events for the specified key. if no list exists, create it
      var collectionEvents: ArrayBuffer[Long] = collectionIds.getOrElse(key, null)
      if(collectionEvents == null) {
        collectionEvents = new ArrayBuffer[Long]()
        collectionIds += (key -> collectionEvents)
      }

      // remove the oldest events until there is room for at least one more event
      while(collectionEvents.size >= maxEventsPerCollection) {
        var idToRemove = collectionEvents.remove(0)
        events -= idToRemove
      }

      // add the event to the event store, add its id to the collection's list, and return the id
      var id = getNextId()
      events += (id -> event)
      collectionEvents += id
      id
  }

  @throws(classOf[IOException])
  override def get(handle: Any): String = synchronized {
    val id: Long = handleToId(handle)
    events.getOrElse(id, null)
  }

  @throws(classOf[IOException])
  override def remove(handle: Any): Unit = synchronized {
    val id: Long = handleToId(handle)
    events -= id
    // be lazy about removing handles from the collectionIds map - this can happen during the
    // getHandles call
  }

  @throws(classOf[IOException])
  def getHandles(projectId: String): HashMap[String, ArrayBuffer[Any]] = synchronized {
    var result = new HashMap[String, ArrayBuffer[Any]]()
    breakable {
      for((key, value) <- collectionIds) {
        // skip collections for different projects
        if(!key.startsWith(projectId)) {
          break
        }

        // extract the collection name from the key
        val eventCollection: String = key.substring(projectId.length + 1)

        // iterate over the list of handles, removing and "dead" events and adding the rest to
        // the result map
        val ids: ArrayBuffer[Long] = value
        var handles: ArrayBuffer[Any] = new ArrayBuffer[Any]()
        val it = ids.iterator
        while(it.hasNext) {
          val id: Long = it.next
          if(events(id) == null) {
            events -= id
          } else {
            handles += (id)
          }
        }

        if(handles.size > 0) {
          result += (eventCollection -> handles)
        }
      }  
    }
    
    result
  }

  def getAttempts(projectId: String, eventCollection: String): String = {
    if(attempts == null) {
      return null
    }

    val project: HashMap[String, String] = attempts.getOrElse(projectId, null)
    if(project == null) {
      return null
    }
    project.getOrElse(eventCollection, null)
  }

  def setAttempts(projectId: String, eventCollection: String, attemptsString: String): Unit = {
    if(attempts == null) {
      attempts = new HashMap[String, HashMap[String, String]]()
    }

    var project: HashMap[String, String] = attempts.getOrElse(projectId, null)
    if(project == null) {
      project = new HashMap[String, String]()
      attempts += (projectId -> project)
    }

    project += (eventCollection -> attemptsString)
  }

  def clear() {
    nextId = 0
    collectionIds = new HashMap[String, ArrayBuffer[Long]]()
    events = new HashMap[Long, String]()
  }

  ///// PRIVATE /////

  private var nextId: Long = 0
  private var collectionIds: HashMap[String, ArrayBuffer[Long]] = new HashMap[String, ArrayBuffer[Long]]()
  private var events: HashMap[Long, String] = new HashMap[Long, String]()
  private var attempts: HashMap[String, HashMap[String, String]] = _

  private def getNextId(): Long = {
    // it should be all but impossible for the event cache to grow bigger than Long.MaxValue,
    // but just for the sake of safe coding practices, check anyway
    if(events.size > Long.MaxValue) {
      throw new IllegalStateException("Event store exceeded maximum size")
    }

    // iterate through ids, starting with the next id counter, until an unused one is found
    var id = nextId
    while(events.getOrElse(id, null) != null) {
      id += 1
    }

    // set the next id to the id that was found plus one, then return the id.
    nextId = id + 1
    id
  }

  private def handleToId(handle: Any): Long = {
    if(!handle.isInstanceOf[Long]) {
      throw new IllegalArgumentException("Expected handle to be a Long, but was: " + handle.getClass.getCanonicalName)
    }
    handle.asInstanceOf[Long]
  }
}