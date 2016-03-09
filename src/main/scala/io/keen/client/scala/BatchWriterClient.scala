package io.keen.client.scala

import java.util.concurrent.{ Executors, ScheduledThreadPoolExecutor, TimeUnit }

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

import com.typesafe.config.{ Config, ConfigFactory }

object BatchWriterClient {
  val MinSendIntervalEvents: Long = 100
  val MaxSendIntervalEvents: Long = 10000
  val MinSendInterval: FiniteDuration = 60.seconds
  val MaxSendInterval: FiniteDuration = 1.hour
}

/**
 * A BatchWriterClient is a [[Client]] specialized with the capability to write
 * events to the Keen API in batches per request.
 *
 * Each instance creates a threadpool and schedules flush operations on it, which
 * will make bulk write calls to the Keen IO API for batches of events until the
 * queue is drained.
 *
 * Events are queued for batch submission with [[queueEvent]]; other operations
 * like [[addEvent]] function as with an ordinary `Client with Writer`—that is,
 * they are non-blocking but effect discrete API calls per invocation.
 *
 * Batch size, flush scheduling, queue bounds, etc. can be tuned via the settings
 * under the `keen.queue` property tree.
 *
 * TODO: explain the difference in behavior if send-interval is zero seconds.
 *
 * @param config Client configuration, by default loaded from `application.conf`.
 */
class BatchWriterClient(config: Config = ConfigFactory.load())
    extends Client(config) with Writer {

  import BatchWriterClient._

  /** The number of events sent in a single API call when flushing batches */
  val batchSize: Integer = settings.batchSize

  /** Timeout for each bulk write API call when flushing batches */
  val batchTimeout: FiniteDuration = settings.batchTimeout

  /** Threshold of queued events at which flush of batches is triggered */
  val sendIntervalEvents: Integer = settings.sendIntervalEvents

  /** Time interval at which batches of queued event writes are scheduled to be flushed */
  val sendInterval: FiniteDuration = settings.sendIntervalDuration

  /** Duration for which client will wait for scheduled batch flushes to complete when shutting down */
  val shutdownDelay: FiniteDuration = settings.shutdownDelay

  // initialize and configure our local event store queue
  // FIXME: should be protected, but tests need to be updated; the EventStore
  // should be an injectable component like HttpAdapter (though the injection
  // pattern still needs work on that too)
  val eventStore: EventStore = new RamEventStore
  eventStore.maxEventsPerCollection = settings.maxEventsPerCollection

  // Schedule sending of queued events.
  protected val scheduledThreadPool: Option[ScheduledThreadPoolExecutor] = scheduleSendQueuedEvents()

  /**
   * Queue an event for batched publishing.
   *
   * @param collection The collection to which the event will be added.
   * @param event The event
   */
  def queueEvent(collection: String, event: String): Unit = {
    // bypass min/max intervals for testing
    // FIXME: There are less kludgey ways to achieve testability here
    environment match {
      case Some("test") if Some("test").get matches "(?i)test" =>
      case _ =>
        require(
          sendIntervalEvents == 0 || (sendIntervalEvents >= MinSendIntervalEvents && sendIntervalEvents <= MaxSendIntervalEvents),
          s"Send events interval must be between $MinSendIntervalEvents and $MaxSendIntervalEvents"
        )
    }

    eventStore.store(projectId, collection, event)

    // If we've met a configured event count threshold, flush the queue.
    if (sendIntervalEvents != 0 && eventStore.size >= sendIntervalEvents) {
      sendQueuedEventsAsync()
    }
  }

  /**
   * Schedule periodic sending of queued events, on a threadpool.
   */
  private def scheduleSendQueuedEvents(): Option[ScheduledThreadPoolExecutor] = {
    // bypass min/max intervals for testing
    environment match {
      case Some("test") if Some("test").get matches "(?i)test" =>
      case _ =>
        require(
          sendInterval.toSeconds == 0 || (sendInterval >= MinSendInterval && sendInterval <= MaxSendInterval),
          s"Send interval must be between $MinSendInterval and $MaxSendInterval"
        )
    }

    // send queued events every n seconds
    sendInterval.toSeconds match {
      case n if n <= 0 => None // TODO: document what config value of zero means
      case _ =>
        // use a thread pool for our scheduled threads so we can use daemon threads
        val tp = Executors.newScheduledThreadPool(1, new ClientThreadFactory).asInstanceOf[ScheduledThreadPoolExecutor]

        // schedule sending from our thread pool at a specific interval
        tp.scheduleWithFixedDelay(new Runnable {
          def run(): Unit = {
            try {
              sendQueuedEvents()
            } catch {
              case ex: Throwable =>
                error("Failed to send queued events")
                error(s"$ex")
            }
          }
        }, 1, sendInterval.toMillis, TimeUnit.MILLISECONDS)

        Some(tp)
    }
  }

  /**
   * Flush queued events, removing them from the queue as they are successfully sent.
   */
  def sendQueuedEvents(): Unit = {
    val handleMap = eventStore.getHandles(projectId)
    val handles = ListBuffer.empty[Long]
    val events = ListBuffer.empty[String]

    // iterate over all of the event handles in the queue, by collection
    for ((collection, eventHandles) <- handleMap) {
      // get each event, and its handle, then add it to a buffer so we can group the events
      // into smaller batches
      for (handle <- eventHandles) {
        handles += handle
        events += eventStore.get(handle)
      }

      // group handles separately so we can use them to remove events from the queue once they've
      // been successfully added
      val handleGroup: List[ListBuffer[Long]] = handles.grouped(batchSize).toList

      // group the events by batch size, then publish them
      for ((batch, index) <- events.grouped(batchSize).zipWithIndex) {
        // publish this batch
        // TODO: Try to avoid this blocking, see https://github.com/keenlabs/KeenClient-Scala/pull/45
        var response = Await.result(
          addEvents(s"""{"$collection": [${batch.mkString(",")}]}"""),
          batchTimeout
        )

        // handle addEvents responses properly
        response.statusCode match {
          case 200 | 201 =>
            info(s"""${response.statusCode} ${response.body} | Sent ${batch.size} queued events""")

            // remove all of the handles for this batch
            for (handle <- handleGroup(index)) {
              eventStore.remove(handle)
            }

            info(s"""Removed ${handleGroup(index).size} events from the queue""")

          // log but DO NOT remove events from queue
          case _ => error(s"""${response.statusCode} ${response.body} | Failed to send ${batch.size} queued events""")
        }
      }
    }
  }

  /**
   * Flush queued events, sending them to Keen IO on a background thread.
   */
  def sendQueuedEventsAsync(): Unit = {
    // use a thread pool for our async thread so we can use daemon threads
    // TODO: Do we really want a daemon thread?
    val tp = Executors.newSingleThreadExecutor(new ClientThreadFactory)

    // send our queued events in a separate thread
    tp.execute(new Runnable {
      def run(): Unit = sendQueuedEvents()
    })
  }

  /**
   * Shut down the threadpool for flushing batch writes, before a final flush of
   * all events remaining in the queue, run on the main thread.
   */
  override def shutdown() = {
    // Shut down the threadpool, if there is one.
    scheduledThreadPool foreach { pool =>
      pool.shutdown()
      val terminated = pool.awaitTermination(shutdownDelay.toMillis, TimeUnit.MILLISECONDS)
      if (!terminated) { error("Failed to shutdown scheduled thread pool") }
    }

    sendQueuedEvents() // flush the queue, on the main thread
    super.shutdown()
  }
}
