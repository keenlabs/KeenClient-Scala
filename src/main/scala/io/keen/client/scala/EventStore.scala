package io.keen.client.scala

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer

/**
 * Abstraction for implementing custom event stores.
 */
trait EventStore {

  var maxEventsPerCollection: Integer = 10000
  var size: Integer = 0

  def store(projectId: String,
    eventCollection: String,
    event: String): Long

  def get(handle: Long): String

  def remove(handle: Long): Unit

  def getHandles(projectId: String): TrieMap[String, ListBuffer[Long]]

}