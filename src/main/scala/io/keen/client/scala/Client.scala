package io.keen.client.scala

import com.ning.http.client.Response
import dispatch._
import Defaults._
import grizzled.slf4j.Logging
import java.net.URL
import scala.concurrent.Promise
import java.nio.charset.StandardCharsets

// XXX These should probably be Options with handling for missing ones below.
class Client(apiURL: String = "https://api.keen.io", version: String = "3.0", masterKey: String, writeKey: String, readKey: String) extends Logging {

  def addEvent(projectId: String, collection: String, event: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events" / collection).secure.setBody(event.getBytes(StandardCharsets.UTF_8))
    doRequest(freq.POST, writeKey)
  }

  def events(projectId: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events").secure
    doRequest(freq.GET, masterKey)
  }

  def getCollection(projectId: String, collection: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events" / collection).secure
    doRequest(freq.GET, masterKey)
  }

  def projects: Future[Response] = {
    val freq = (url(apiURL) / version / "projects").secure
    doRequest(freq.GET, masterKey)
  }

  def project(projectId: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId).secure
    doRequest(freq.GET, masterKey)
  }

  /**
   * Perform the request with some debugging for good measure.
   *
   * @param req The request
   */
  private def doRequest(req: Req, key: String) = {
    val breq = req.toRequest
    debug("%s: %s".format(breq.getMethod, breq.getUrl))
    Http(
      req.setHeader("Content-type", "application/json; charset=utf-8")
        // Set the provided key, for authentication.
        .setHeader("Authorization", key)
    )
  }
}

object Client {
  /**
   * Disconnects any remaining connections. Both idle and active. If you are accessing
   * Keen through a proxy that keeps connections alive this is useful.
   *
   * If your application uses the dispatch library for other purposes, those connections
   * will also terminate.
   */
  def shutdown {
    Http.shutdown()
  }

}
