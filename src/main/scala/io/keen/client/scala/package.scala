package io.keen.client

import com.typesafe.config.Config

package object scala {
  /**
   * Enrichment for Typesafe Config to wrap some optional settings in Option.
   *
   * @internal Could use [[https://github.com/ceedubs/ficus Ficus]] for fancy
   * stuff, but for simple Options for now this is lighter.
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
  }
}

package scala {
  case class MissingCredential(cause: String) extends RuntimeException(cause)
}
