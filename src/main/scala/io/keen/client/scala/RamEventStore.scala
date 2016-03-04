package io.keen.client.scala

import java.util.Locale

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

/**
 * In-memory queue for [[Client]].
 */
class RamEventStore extends AttemptCountingEventStore {

  private var nextId: Long = 0
  private var collectionIds: TrieMap[String, ListBuffer[Long]] = new TrieMap[String, ListBuffer[Long]]()
  private var events: TrieMap[Long, String] = new TrieMap[Long, String]()
  private var attempts: TrieMap[String, TrieMap[String, String]] = _

  /**
   * Event store.
   *
   * @param projectId       The ID of the project to which the collection belongs.
   * @param eventCollection The collection to which the event will be added.
   * @param event           The event.
   */
  override def store(projectId: String,
    eventCollection: String,
    event: String): Long = synchronized {

      // create a key from the project ID and event collection
      val key = "%s$%s".formatLocal(Locale.US, projectId, eventCollection)

      // get the list of events for the specified key. if no list exists, create it
      var collectionEvents: ListBuffer[Long] = collectionIds.getOrElse(key, null)
      if(collectionEvents == null) {
        collectionEvents = new ListBuffer[Long]()
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
      size = events.size
      collectionEvents += id
      id
  }

  /**
   * Retrieves a specific event from the store.
   *
   * @param handle The handle of the event.
   * @return       The event string.
   */
  override def get(handle: Long): String = synchronized {
    val id: Long = handleToId(handle)
    events.getOrElse(id, null)
  }

  /**
   * Removes a specific event from the store.
   *
   * @param handle The handle of the event.
   */
  override def remove(handle: Long): Unit = synchronized {
    val id: Long = handleToId(handle)
    events -= id
    size = events.size
    // be lazy about removing handles from the collectionIds map - this can happen during the
    // getHandles call
  }

  /**
   * Retrieves all handles for a specific project from the store.
   *
   * @param projectId The ID of the project.
   * @return          A map of collection names and their events.
   */
  def getHandles(projectId: String): TrieMap[String, ListBuffer[Long]] = synchronized {
    
    var result = new TrieMap[String, ListBuffer[Long]]()
    
    for((key, value) <- collectionIds) {

      breakable {

        // skip collections for different projects
        if(!key.startsWith(projectId)) {
          break
        }

        // extract the collection name from the key
        val eventCollection: String = key.substring(projectId.length + 1)

        // iterate over the list of handles, removing and "dead" events and adding the rest to
        // the result map
        val ids: ListBuffer[Long] = value
        var handles: ListBuffer[Long] = new ListBuffer[Long]()
        for(id <- ids) {
          if(!events.keySet.exists(_ == id)) {
            // lazily remove the "dead" event
            ids -= id
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

    val project: TrieMap[String, String] = attempts.getOrElse(projectId, null)
    if(project == null) {
      return null
    }
    project.getOrElse(eventCollection, null)
  }

  def setAttempts(projectId: String, eventCollection: String, attemptsString: String): Unit = {
    if(attempts == null) {
      attempts = new TrieMap[String, TrieMap[String, String]]()
    }

    var project: TrieMap[String, String] = attempts.getOrElse(projectId, null)
    if(project == null) {
      project = new TrieMap[String, String]()
      attempts += (projectId -> project)
    }

    project += (eventCollection -> attemptsString)
  }

  def clear(): Unit = {
    nextId = 0
    collectionIds = new TrieMap[String, ListBuffer[Long]]()
    events = new TrieMap[Long, String]()
  }

  ///// PRIVATE /////  

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

  private def handleToId(handle: Long): Long = {
    if(!handle.isInstanceOf[Long]) {
      throw new IllegalArgumentException("Expected handle to be a Long, but was: " + handle.getClass.getCanonicalName)
    }
    handle.asInstanceOf[Long]
  }

}