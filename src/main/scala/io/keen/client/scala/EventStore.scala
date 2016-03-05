package io.keen.client.scala

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer

/**
 * An EventStore is an abstract store for events, intended primarily as a write
 * cache for events to be flushed in batches, such as by a [[BatchWriterClient]].
 *
 * The documentation refers to "handles", which you can think of simply as cache
 * keys or map keys in common parlance. The parameter naming is historical.
 *
 * @todo I'd like to change parameter names to `key` before 1.0.
 * @todo The handle/key type could be generic instead of `Long`.
 * @todo The data structure should be generalized from `TrieMap` & `ListBuffer`
 */
trait EventStore {
  /**
   * Sets a maximum number of events that should be stored per-collection.
   * Eviction strategy may vary by implementation.
   *
   * @todo This could be moved to RamEventStore instead of being a formal part
   *   of the interface.
   */
  var maxEventsPerCollection: Integer = 10000

  /**
   * The number of events currently in the store.
   *
   * @todo This should be an accessor method with private field, not public var.
   * @todo Long? The implementation in RamEventStore uses Longs for key/handle/ID, so…
   */
  var size: Integer = 0

  /**
   * Stores an event in the store.
   *
   * @param projectId       The ID of the project to which the collection belongs.
   * @param eventCollection The collection to which the event will be added.
   * @param event           The event.
   *
   * @return A handle which can be used to retrieve or remove the event from the
   *   store (a cache key).
   */
  def store(projectId: String, eventCollection: String, event: String): Long

  /**
   * Retrieves an event from the store.
   *
   * @param handle The cache key of the event to retrieve.
   * @return       The event string.
   *
   * @todo Should return an Option[String] in Scala land.
   * @todo Non-Option instance apply() version could be added for idiomatic Scala.
   */
  def get(handle: Long): String

  /**
   * Removes an event from the store.
   *
   * @param handle The handle of the event.
   */
  def remove(handle: Long): Unit

  /**
   * Retrieves all handles for a specific project from the store.
   *
   * @param projectId The ID of the project.
   * @return          A map of collection names to their event handles.
   */
  def getHandles(projectId: String): TrieMap[String, ListBuffer[Long]]
}
