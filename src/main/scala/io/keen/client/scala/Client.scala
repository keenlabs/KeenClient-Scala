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
   * Returns the number of resources in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param events The events to add to the project.
   */
  def count(projectId: String, collection: String, filters: Option[String] = None, timeframe: Option[String] = None): Future[Response] = {
    val req = (url(apiURL) / version / "projects" / projectId / "queries" / "count").secure.addQueryParameter("event_collection", collection)

    val paramNames = List("filters", "timeframe")
    val params = List(filters, timeframe)

    // Since modifying a dispatch request makes a copy, we use some convenient functional
    // bits.  First, zip together the paramNames and their values.
    val reqWithparams = paramNames.zip(params)
      // Now, filter out any Tuples with a None (eliminating any unspecified params)
      .filter(_._2.isDefined)
      // Finally, foldLeft each remaining tuple, modifying the request. foldLeft will return
      // each iteration's return value meaning that the final iteration returns the value
      // we use in reqWithParams
      .foldLeft(req)((r, nameAndParam) => r.addQueryParameter(nameAndParam._1, nameAndParam._2.toString))

    doRequest(reqWithparams.GET, readKey)
  }

  /**
   * Deletes the entire event collection. This is irreversible and will only work for collections under 10k events. See [[https://keen.io/docs/api/reference/#event-collection-resource Event Collection Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the collection.
   */
  def deleteCollection(projectId: String, collection: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events" / collection).secure
    doRequest(freq.DELETE, masterKey)
  }

  /**
   * Removes a property and deletes all values stored with that property name. See [[https://keen.io/docs/api/reference/#property-resource Property Resource]].
   */
  def deleteProperty(projectId: String, collection: String, name: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events" / collection / "properties" / name).secure
    doRequest(freq.DELETE, masterKey)
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
   * Returns the projects accessible to the API user, as well as links to project sub-resources for discovery. See [[https://keen.io/docs/api/reference/#property-resource Property Resource]].
   */
  def getProperty(projectId: String, collection: String, name: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events" / collection / "properties" / name).secure
    doRequest(freq.GET, masterKey)
  }

  /** 
   * Returns the list of available queries and links to them. See [[https://keen.io/docs/api/reference/#queries-resource Queries Resource]].
   *
   * @param projectID The project to which the event will be added.
   */
  def getQueries(projectId: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "queries").secure
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
