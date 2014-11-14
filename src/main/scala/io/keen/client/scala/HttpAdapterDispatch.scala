package io.keen.client.scala

import dispatch.Defaults._
import dispatch._

import scala.concurrent.Future

/**
 * Extension of HttpAdapter that uses Dispatch rather than Spray+akka
 * Helps avoid dependency conflicts in use cases such as Spark
 */
class HttpAdapterDispatch(httpTimeoutSeconds: Int = 10) extends HttpAdapter {
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
    val result = Http(httpRequestWithHeader)

    result map (r => Response(r.getStatusCode, r.getResponseBody))
  }

  def shutdown = {}
}
