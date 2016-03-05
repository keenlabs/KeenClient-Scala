package io.keen.client.scala

import dispatch.Defaults._
import dispatch._

import scala.concurrent.Future

/**
 * An [[HttpAdapter]] built on the [[http://dispatch.databinder.net/ Dispatch]]
 * HTTP client library.
 *
 * @param httpTimeoutSeconds Sets a timeout for HTTP requests, in seconds.
 * @todo Move the timeout constructor param to config; it's not used here!
 */
class HttpAdapterDispatch(httpTimeoutSeconds: Int = 10) extends HttpAdapter {
  val http = new Http

  def doRequest(
    scheme: String,
    authority: String,
    path: String,
    method: String,
    key: String,
    body: Option[String] = None,
    params: Map[String, Option[String]] = Map.empty
  ): Future[Response] = {

    val filteredParams =
      params
        .filter(_._2.isDefined)
        .map(param => param._1 -> param._2.get)

    val finalUrl = url(s"$scheme://$authority/$path") <<? filteredParams

    // Optionally attach body
    val finalUrlWithBody = body match {
      case Some(s) => finalUrl.setContentType("application/json", "UTF-8") << s
      case _       => finalUrl
    }

    // Create request
    val httpRequest = method match {
      case "DELETE" => finalUrlWithBody.DELETE
      case "GET"    => finalUrlWithBody.GET
      case "POST"   => finalUrlWithBody.POST
      case _        => throw new IllegalArgumentException("Unknown HTTP method: " + method)
    }

    // Add headers
    val httpRequestWithHeader = httpRequest.addHeader("Authorization", key)

    // Fire request
    val result = http(httpRequestWithHeader)

    result map (r => Response(r.getStatusCode, r.getResponseBody))
  }

  def shutdown() = http.shutdown()

  // TODO: This is dubious...
  override protected def finalize() = shutdown()
}
