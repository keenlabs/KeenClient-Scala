package io.keen.client.scala

trait AttemptCountingEventStore extends EventStore {
  
  def getAttempts(projectId: String, eventCollection: String): String

  def setAttempts(projectId: String, eventCollection: String, attemptsString: String): Unit

}