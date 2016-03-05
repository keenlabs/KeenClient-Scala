package io.keen.client.scala

import scala.concurrent.duration.FiniteDuration

import com.typesafe.config._

/**
 * Configuration settings for Keen IO [[Client]].
 *
 * Settings can be parsed from config files, properties, environment variables, etc.
 * as supported by [[https://github.com/typesafehub/config Typesafe Config]].
 *
 * @constructor Creates a new Settings instance, validating that all required
 *   keys are present in the given [[com.typesafe.config.Config]].
 *
 * @param config A Typesafe `Config` instance containing at least the required
 *   settings under the "keen" path reflected in the reference file.
 *
 * @throws ConfigException.Missing if required configuration keys are not provided.
 * @throws ConfigException.WrongType if a given configuration value is not of
 *   the required or sensibly coercible type.
 *
 * @groupdesc Keys [[https://keen.io/docs/api/#api-keys API Keys]]
 * @groupdesc Batch Settings for batch write functionality, such as implemented
 *   in [[BatchWriterClient]].
 */
class Settings(config: Config) {
  // Fail fast if any required settings are missing.
  private val validationReference = ConfigFactory.defaultReference.withoutPath("keen.optional")
  config.checkValid(validationReference, "keen")

  // format: OFF

  /** Keen IO project ID that clients will operate on by default.
    * From configuration key `keen.project-id`. */
  val projectId: String = config.getString("keen.project-id")

  val environment: Option[String] = config.getOptionalString("keen.optional.environment")

  /** @group Keys */
  val masterKey: Option[String] = config.getOptionalString("keen.optional.master-key")
  /** @group Keys */
  val readKey: Option[String]   = config.getOptionalString("keen.optional.read-key")
  /** @group Keys */
  val writeKey: Option[String]  = config.getOptionalString("keen.optional.write-key")

  // TODO: These batch field names could use some kind of namespacing

  /** The number of events sent in a single API call when flushing batches.
    * From configuration key `keen.queue.batch.size`.
    * @group Batch */
  val batchSize: Integer                   = config.getInt("keen.queue.batch.size")

  /** Timeout for each bulk write API call when flushing batches.
    * From configuration key `keen.queue.batch.timeout`.
    * @group Batch */
  val batchTimeout: FiniteDuration         = config.getFiniteDuration("keen.queue.batch.timeout")

  /** Default queue bound to limit events stored in a write cache for batched writes.
    * From configuration key `keen.queue.queue.max-events-per-collection`.
    * @group Batch */
  val maxEventsPerCollection: Integer      = config.getInt("keen.queue.max-events-per-collection")

  /** Threshold of queued events at which flush of batches is triggered.
    * From configuration key `keen.queue.send-interval.events`.
    * @group Batch */
  val sendIntervalEvents: Integer          = config.getInt("keen.queue.send-interval.events")

  /** Time interval at which batches of queued event writes are scheduled to be flushed.
    * From configuration key `keen.queue.send-interval.duration`.
    * @group Batch */
  val sendIntervalDuration: FiniteDuration = config.getFiniteDuration("keen.queue.send-interval.duration")

  /** Duration for which client will wait for scheduled batch flushes to complete when shutting down.
    * From configuration key `keen.queue.shutdown-delay`.
    * @group Batch */
  val shutdownDelay: FiniteDuration        = config.getFiniteDuration("keen.queue.shutdown-delay")
}
