package io.keen.client

import concurrent.duration.{ FiniteDuration, MILLISECONDS }

import com.typesafe.config.Config

package object scala {
  /**
   * Enrichment for Typesafe Config to wrap some optional settings in Option, or
   * get Scala `FiniteDuration` values instead of `java.time.Duration`.
   *
   * @internal Could use [[https://github.com/iheartradio/ficus Ficus]] for
   *   fancier stuff, but for a few simple needs this is lighter for now.
   */
  implicit class RichConfig(val self: Config) extends AnyVal {
    def getOptionalString(path: String): Option[String] = {
      if (self.hasPath(path)) Some(self.getString(path))
      else None
    }
    def getOptionalInt(path: String): Option[Integer] = {
      if (self.hasPath(path)) Some(self.getInt(path))
      else None
    }
    def getFiniteDuration(path: String): FiniteDuration = {
      val millis = self.getDuration(path, java.util.concurrent.TimeUnit.MILLISECONDS)
      FiniteDuration(millis, MILLISECONDS)
    }
  }
}

package scala {
  case class MissingCredential(cause: String) extends RuntimeException(cause)
}
