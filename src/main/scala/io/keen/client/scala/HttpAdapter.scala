package io.keen.client.scala

import scala.concurrent.Future

case class Response(statusCode: Int, body: String)

/**
 * Expresses a dependency on an [[HttpAdapter]], which an implementor or library
 * user must provide.
 */
trait HttpAdapterComponent {
  val httpAdapter: HttpAdapter
}

/**
 * A basic HTTP client abstraction. This interface allows pluggable use of new
 * HTTP client libraries of your choice for the Keen [[Client]].
 */
trait HttpAdapter {
  /**
   * Perform an HTTP request.
   *
   * @todo Not even documenting the params yet because I'm in the midst of
   *   refactoring the hell out of this :-)
   */
  def doRequest(
    scheme: String,
    authority: String,
    path: String,
    method: String,
    key: String,
    body: Option[String] = None,
    params: Map[String, Option[String]] = Map.empty
  ): Future[Response]

  /** Shuts down HTTP client resources. */
  def shutdown(): Unit
}
