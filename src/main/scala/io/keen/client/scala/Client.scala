package io.keen.client.scala

import grizzled.slf4j.Logging
import scala.concurrent.Future
import java.nio.charset.StandardCharsets

// XXX Remaining: Extraction, Funnel, Saved Queries List, Saved Queries Row, Saved Queries Row Result
// Event deletion

// XXX These should probably be Options with handling for missing ones below.
class Client(
  scheme: String = "https",
  authority: String = "api.keen.io",
  version: String = "3.0",
  projectId: String,
  masterKey: String,
  writeKey: String,
  readKey: String,
  httpAdapter: HttpAdapter = new HttpAdapter()) extends Logging {

  /**
   * Publish a single event. See [[https://keen.io/docs/api/reference/#event-collection-resource Event Collection Resource]].
   *
   * @param collection The collection to which the event will be added.
   * @param event The event
   */
  def addEvent(collection: String, event: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection).mkString("/")
    doRequest(path = path, method = "POST", key = writeKey, body = Some(event))
  }

  /**
   * Publish multiple events. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param events The events to add to the project.
   */
  def addEvents(events: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events").mkString("/")
    doRequest(path = path, method = "POST", key = writeKey, body = Some(events))
  }

  /**
   * Returns the average across all numeric values for the target property in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#average-resource Average Resource]].
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def average(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] =

    doQuery(
      analysisType = "average",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

  /**
   * Returns the number of resources in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param collection The name of the event collection you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   */
  def count(
    collection: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] =

    doQuery(
      analysisType = "count",
      collection = collection,
      targetProperty = None,
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

  /**
   * Returns the number of '''unique''' resources in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def countUnique(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] =

    doQuery(
      analysisType = "count",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

  /**
   * Returns the maximum numeric value for the target property in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#maximum-resource Maximum Resource]].
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def maximum(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] =

    doQuery(
      analysisType = "maximum",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

  /**
   * Returns the minimum numeric value for the target property in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#minimum-resource Minimum Resource]].
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def minimum(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] =

    doQuery(
      analysisType = "minimum",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

 /**
   * Returns a list of '''unique''' resources in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#select-unique-resource Select Unique Resource]].
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def selectUnique(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] =

    doQuery(
      analysisType = "select_unique",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

  /**
   * Returns the sum across all numeric values for the target property in the event collection matching the given criteria. See [[https://keen.io/docs/api/reference/#sum-resource Sum Resource]].
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   */
  def sum(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] =

    doQuery(
      analysisType = "sum",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy)

  /**
   * Deletes the entire event collection. This is irreversible and will only work for collections under 10k events. See [[https://keen.io/docs/api/reference/#event-collection-resource Event Collection Resource]].
   *
   * @param collection The name of the collection.
   */
  def deleteCollection(collection: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection).mkString("/")
    doRequest(path = path, method = "DELETE", key = masterKey)
  }

  /**
   * Removes a property and deletes all values stored with that property name. See [[https://keen.io/docs/api/reference/#property-resource Property Resource]].
   */
  def deleteProperty(collection: String, name: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection, "properties", name).mkString("/")
    doRequest(path = path, method = "DELETE", key = masterKey)
  }

  /**
   * Returns schema information for all the event collections in this project. See [[https://keen.io/docs/api/reference/#event-resource Event Resource]].
   *
   * @param projectID The project to which the event will be added.
   */
  def getEvents: Future[Response] = {
    val path = Seq(version, "projects", projectId, "events").mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns available schema information for this event collection, including properties and their type. It also returns links to sub-resources. See [[https://keen.io/docs/api/reference/#event-collection-resource Event Collection Resource]].
   *
   * @param projectID The project to which the event will be added.
   * @param collection The name of the collection.
   */
  def getCollection(collection: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection).mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns the projects accessible to the API user, as well as links to project sub-resources for
   * discovery. See [[https://keen.io/docs/api/reference/#projects-resource Projects Resource]].
   */
  def getProjects: Future[Response] = {
    val path = Seq(version, "projects").mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns detailed information about the specific project, as well as links to related resources.
   * See [[https://keen.io/docs/api/reference/#project-row-resource Project Row Resource]].
   */
  def getProject: Future[Response] = {
    val path = Seq(version, "projects", projectId).mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns the property name, type, and a link to sub-resources. See [[https://keen.io/docs/api/reference/#property-resource Property Resource]].
   */
  def getProperty(collection: String, name: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection, "properties", name).mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns the list of available queries and links to them. See [[https://keen.io/docs/api/reference/#queries-resource Queries Resource]].
   *
   */
  def getQueries: Future[Response] = {
    val path = Seq(version, "projects", projectId, "queries").mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  private def doQuery(
    analysisType: String,
    collection: String,
    targetProperty: Option[String],
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String]= None): Future[Response] = {

    val path = Seq(version, "projects", projectId, "queries", analysisType).mkString("/")

    val params = Map(
      "event_collection" -> Some(collection),
      "target_property" -> targetProperty,
      "filters" -> filters,
      "timeframe" -> timeframe,
      "timezone" -> timezone,
      "group_by" -> groupBy
    )

    doRequest(path = path, method = "GET", key = readKey, params = params)
  }

  private def doRequest(
    path: String,
    method: String,
    key: String,
    body: Option[String] = None,
    params: Map[String,Option[String]] = Map.empty) = {

    httpAdapter.doRequest(method = method, scheme = scheme, authority = authority, path = path, key = key, body = body, params = params)
  }


  /**
   * Disconnects any remaining connections. Both idle and active. If you are accessing
   * Keen through a proxy that keeps connections alive this is useful.
   */
  def shutdown {
    httpAdapter.shutdown
  }
}
