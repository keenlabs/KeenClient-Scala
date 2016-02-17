package io.keen.client.scala

import java.io.IOException

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

trait KeenEventStore {

  @throws(classOf[IOException])
  def store(projectId: String,
    eventCollection: String,
    event: String): Any

  @throws(classOf[IOException])
  def get(handle: Any): String


  // @throws(classOf[IOException])
  // def remove(handle: Any): Unit

  @throws(classOf[IOException])
  def getHandles(projectId: String): HashMap[String, ArrayBuffer[Any]]
}