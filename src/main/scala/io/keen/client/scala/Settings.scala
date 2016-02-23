package io.keen.client.scala

import com.typesafe.config._

/**
 * Configuration settings for Keen Client.
 *
 * At construction the given `Config` parsed from config files, properties, etc.
 * is validated to ensure that all required keys are present.
 *
 * @constructor Creates a new Settings instance.
 *
 * @param config A Typesafe `Config` instance containing at least the required
 *   settings under the "keen" path reflected in the reference file.
 *
 * @throws ConfigException.Missing if required configuration keys are not provided.
 * @throws ConfigException.WrongType if a given configuration value is not of
 *   the required or sensibly coercible type.
 *
 * @todo A helpful toString could be useful here, but should maybe sanitize keys
 *   from accidental logging through exceptions reports, etc.?
 */
class Settings(config: Config) {
  // Fail fast if any required settings are missing.
  private val validationReference = ConfigFactory.defaultReference.withoutPath("keen.optional")
  config.checkValid(validationReference, "keen")

  val projectId: String = config.getString("keen.project-id")

  val masterKey: Option[String] = config.getOptionalString("keen.optional.master-key")
  val readKey: Option[String]   = config.getOptionalString("keen.optional.read-key")
  val writeKey: Option[String]  = config.getOptionalString("keen.optional.write-key")

  val batchSize: Option[Integer] = config.getOptionalInt("keen.optional.queue.batch.size")
  val batchTimeout: Option[Integer] = config.getOptionalInt("keen.optional.queue.batch.timeout")
  val maxEventsPerCollection: Option[Integer] = config.getOptionalInt("keen.optional.queue.max-events-per-collection")
  val sendIntervalEvents: Option[Integer] = config.getOptionalInt("keen.optional.queue.send-interval.events")
  val sendIntervalSeconds: Option[Integer] = config.getOptionalInt("keen.optional.queue.send-interval.seconds")
}

