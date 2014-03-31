package io.keen.client.scala

import dispatch._
import grizzled.slf4j.Logging
import scala.concurrent.ExecutionContext.Implicits.global

class HttpAdapter() extends Logging {

  /**
   * Perform the request with some debugging for good measure.
   *
   * @param req The request
   */
  def doRequest(req: Req, key: String): Future[Response] = {
    val breq = req.toRequest
    debug("%s: %s".format(breq.getMethod, breq.getUrl))
    Http.configure(_.setConnectionTimeoutInMs(5000))(
      req.setHeader("Content-type", "application/json; charset=utf-8")
        // Set the provided key, for authentication.
        .setHeader("Authorization", key)
    ) map { res =>
      Response(statusCode = res.getStatusCode, body =res.getResponseBody)
    }
  }
}
