package io.keen.client.scala

import java.io.IOException

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

trait KeenEventStore {

  @throws(classOf[IOException])
  def store(projectId: String,
    eventCollection: String,
    event: String): Long

  @throws(classOf[IOException])
  def get(handle: Long): String

  @throws(classOf[IOException])
  def remove(handle: Long): Unit

  @throws(classOf[IOException])
  def getHandles(projectId: String): HashMap[String, ListBuffer[Long]]

}