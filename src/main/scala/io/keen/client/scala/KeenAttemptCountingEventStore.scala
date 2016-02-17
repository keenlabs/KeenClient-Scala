package io.keen.client.scala

import java.io.IOException

trait KeenAttemptCountingEventStore extends KeenEventStore {
  
  @throws(classOf[IOException])
  def getAttempts(projectId: String, eventCollection: String): String

  @throws(classOf[IOException])
  def setAttempts(projectId: String, eventCollection: String, attemptsString: String): Unit

}