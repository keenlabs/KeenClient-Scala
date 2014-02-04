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
    val freq = (url(apiURL) / version / "projects" / projectId / "events" / collection).secure
      .setBody(event.getBytes(StandardCharsets.UTF_8))
    doRequest(freq.POST, writeKey)
  }

  /**
   * Publish multiple events. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param events The events to add to the project.
   */
  def addEvents(projectId: String, events: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId / "events").secure
      .setBody(events.getBytes(StandardCharsets.UTF_8))
    doRequest(freq.POST, writeKey)
  }

  /**
   * Returns the average across all numeric values for the target property in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#average-resource Average Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def average(
    projectId: String,
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] = 

    doQuery(
      query = "average",
      projectId = projectId,
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

  /**
   * Returns the number of resources in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the event collection you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   */
  def count(
    projectId: String,
    collection: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None): Future[Response] = {

    doQuery(
      query = "count",
      projectId = projectId,
      collection = collection,
      targetProperty = None,
      filters = filters,
      timeframe = timeframe,
      timezone = None,
      groupBy = None)
  }

  /**
   * Returns the number of '''unique''' resources in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def countUnique(
    projectId: String,
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] = {

    doQuery(
      query = "count",
      projectId = projectId,
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)
  }

  /**
   * Returns the maximum numeric value for the target property in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#maximum-resource Maximum Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def maximum(
    projectId: String,
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] = 

    doQuery(
      query = "maximum",
      projectId = projectId,
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

  /**
   * Returns the minimum numeric value for the target property in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#minimum-resource Minimum Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def minimum(
    projectId: String,
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] = 

    doQuery(
      query = "minimum",
      projectId = projectId,
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

 /**
   * Returns a list of '''unique''' resources in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#select-unique-resource Select Unique Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def selectUnique(
    projectId: String,
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] = {

    doQuery(
      query = "select_unique",
      projectId = projectId,
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)
  }

  /**
   * Returns the sum across all numeric values for the target property in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#sum-resource Sum Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def sum(
    projectId: String,
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] = 

    doQuery(
      query = "sum",
      projectId = projectId,
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

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
   * Returns the projects accessible to the API user, as well as links to project sub-resources for
   * discovery. See [[https://keen.io/docs/api/reference/#projects-resource Projects Resource]].
   */
  def getProjects: Future[Response] = {
    val freq = (url(apiURL) / version / "projects").secure
    doRequest(freq.GET, masterKey)
  }

  /**
   * Returns detailed information about the specific project, as well as links to related resources.
   * See [[https://keen.io/docs/api/reference/#project-row-resource Project Row Resource]].
   */
  def getProject(projectId: String): Future[Response] = {
    val freq = (url(apiURL) / version / "projects" / projectId).secure
    doRequest(freq.GET, masterKey)
  }

  /**
   * Returns the property name, type, and a link to sub-resources. See [[https://keen.io/docs/api/reference/#property-resource Property Resource]].
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

  private def doQuery(
    query: String,
    projectId: String,
    collection: String,
    targetProperty: Option[String],
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] = {

    val req = (url(apiURL) / version / "projects" / projectId / "queries" / query).secure
      .addQueryParameter("event_collection", collection)

    val paramNames = List("target_property", "filters", "timeframe", "timezone", "group_by")
    val params = List(targetProperty, filters, timeframe, timezone, groupBy)

    val reqWithParams = parameterizeUrl(req, paramNames, params)
    doRequest(reqWithParams.GET, readKey)
  }

  /**
   * Slap request params onto a URL, filtering out Nones.
   * 
   * @param req The request to use
   * @param names List of parameter names.
   * @param values List of parameter values.
   */
  private def parameterizeUrl(req: Req, names: List[String], values: List[Option[String]]): Req = {

    // Since modifying a dispatch request makes a copy, we use some convenient functional
    // bits.  First, zip together the paramNames and their values.
    names.zip(values)
      // Now, filter out any Tuples with a None (eliminating any unspecified params)
      .filter(_._2.isDefined)
      // Finally, foldLeft each remaining tuple, modifying the request. foldLeft will return
      // each iteration's return value meaning that the final iteration returns the value
      // we use in reqWithParams.
      .foldLeft(req)((r, nameAndParam) => r.addQueryParameter(nameAndParam._1, nameAndParam._2.get))
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
