package io.keen.client.scala

trait KeenAttemptCountingEventStore extends KeenEventStore {
  var attempts: Integer = 0
}