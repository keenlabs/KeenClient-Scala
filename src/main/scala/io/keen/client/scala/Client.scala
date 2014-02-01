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

  /**
   * Publish a single event. See [[https://keen.io/docs/api/reference/#event-collection-resource Event Collection Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The collection to which the event will be added.
   * @param event The event
   */
  def addEvent(projectId: String, collection: String, event: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events" / collection).secure.setBody(event.getBytes(StandardCharsets.UTF_8))
    doRequest(freq.POST, writeKey)
  }

  /**
   * Publish multiple events. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param events The events to add to the project.
   */
  def addEvents(projectId: String, events: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events").secure.setBody(events.getBytes(StandardCharsets.UTF_8))
    doRequest(freq.POST, writeKey)
  }

  /**
   * Returns schema information for all the event collections in this project. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param projectID The project to which the event will be added.
   */
  def getEvents(projectId: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events").secure
    doRequest(freq.GET, masterKey)
  }

  /**
   * Returns available schema information for this event collection, including properties and their type. It also returns links to sub-resources. See [[https://keen.io/docs/api/reference/#event-collection-resource Event Collection Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the collection.
   */
  def getCollection(projectId: String, collection: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events" / collection).secure
    doRequest(freq.GET, masterKey)
  }

  /**
   * Returns the projects accessible to the API user, as well as links to project sub-resources for discovery. See [[https://keen.io/docs/api/reference/#projects-resource Projects Resource]].
   */
  def getProjects: Future[Response] = {
    val freq = (url(apiURL) / version / "projects").secure
    doRequest(freq.GET, masterKey)
  }

  /**
   * Returns the projects accessible to the API user, as well as links to project sub-resources for discovery. See [[https://keen.io/docs/api/reference/#project-row-resource Project Row Resource]].
   */
  def getProject(projectId: String): Future[Response] = {
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
