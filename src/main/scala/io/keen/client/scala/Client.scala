package io.keen.client.scala

import scala.concurrent.Future

import com.typesafe.config.{ Config, ConfigFactory }
import grizzled.slf4j.Logging

// XXX Remaining: Extraction, Funnel, Saved Queries List, Saved Queries Row, Saved Queries Row Result
// Event deletion

class Client(
  config: Config = ConfigFactory.load(),
  // These will move to a config file:
  val scheme: String = "https",
  val authority: String = "api.keen.io",
  val version: String = "3.0"
  ) extends HttpAdapterComponent with Logging {

  val httpAdapter: HttpAdapter = new HttpAdapterSpray
  val settings: Settings = new Settings(config)

  /**
   * Disconnects any remaining connections. Both idle and active. If you are accessing
   * Keen through a proxy that keeps connections alive this is useful.
   */
  def shutdown() = {
    httpAdapter.shutdown()
  }
}

/**
 * A [[Client]] can mix in one or more `AccessLevel`s to enable API calls for
 * read, write, and master operations.
 *
 * The intention of this approach is to make it a compile-time error to call an
 * API method requiring a write key if you haven't statically declared that the
 * client should be a writer, for example.
 *
 * This also means that runtime checks for presence of optional settings (keys
 * for access levels you don't need) are pushed up to the time of client
 * instantiation: if you've forgotten to provide a write key in your deployment
 * environment, we won't wait to throw a runtime exception at the point that you
 * make a write call, perhaps long after your app has started and you've gone
 * home for the weekend.
 *
 * @example Client with read and write access:
 * {{{
 * val keen = new Client with Reader with Writer
 * }}}
 *
 * @see [[https://keen.io/docs/security/]]
 */
sealed protected trait AccessLevel {
  // Access levels need the basic facilities of a Client; require that.
  self: Client =>

  val projectId: String = settings.projectId

  // TODO: These don't belong here abstraction-wise. They will move to a config
  // file and will be properties of HttpAdapter so that doRequest does not need
  // to be defined in this trait just to pass these through. The other traits
  // will then be able to use httpAdapter.doRequest instead of this inherited
  // version.
  val scheme: String
  val authority: String
  val version: String

  protected def doRequest(
    path: String,
    method: String,
    key: String,
    body: Option[String] = None,
    params: Map[String, Option[String]] = Map.empty) = {

    httpAdapter.doRequest(method = method, scheme = scheme, authority = authority, path = path, key = key, body = body, params = params)
  }
}

/**
 * A [[Client]] mixing in `Reader` can make Keen IO API calls requiring a read
 * key.
 *
 * A read key must be configured in the `Client`'s [[Settings]] or the `readKey`
 * field must otherwise be set e.g. with an anonymous class override.
 *
 * @example Initializing a Client with read access
 * {{{
 * val keen = new Client with Reader {
 *   override val readKey = "myReadKey"
 * }
 * }}}
 *
 * @throws MissingCredential if a read key is not configured.
 *
 * @see [[https://keen.io/docs/security/]]
 */
trait Reader extends AccessLevel {
  self: Client =>

  /**
   * A read key required to make API calls for querying and extracting data.
   */
  val readKey: String = settings.readKey.getOrElse(throw MissingCredential("Read key required for Reader"))

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
}

/**
 * A [[Client]] mixing in `Writer` can make Keen IO API calls requiring a write
 * key.
 *
 * A write key must be configured in the `Client`'s [[Settings]] or the
 * `writeKey` field must otherwise be set e.g. with an anonymous class override.
 *
 * @example Initializing a Client with write access
 * {{{
 * val keen = new Client with Writer {
 *   override val writeKey = "myWriteKey"
 * }
 * }}}
 *
 * @throws MissingCredential if a write key is not configured.
 *
 * @see [[https://keen.io/docs/security/]]
 */
trait Writer extends AccessLevel {
  self: Client =>

  /**
   * A write key required to make API calls that write data.
   */
  val writeKey: String = settings.writeKey.getOrElse(throw MissingCredential("Write key required for Writer"))

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
}

/**
 * A [[Client]] mixing in `Master` can make Keen IO API calls requiring a master
 * key, such as deleting data, creating saved queries, and performing
 * administrative functions.
 *
 * A `Master` client can also perform all [[Reader]] and [[Writer]] API calls
 * and does not require additional keys configured for these. However, this
 * should '''not''' be considered a shortcut! Please keep your master key as
 * secure as possible by not deploying it where it isn't strictly needed.
 *
 * A master key must be configured in the `Client`'s [[Settings]] or the
 * `masterKey` field must otherwise be set e.g. with an anonymous class override.
 *
 * @example Initializing a Client with master access
 * {{{
 * val keen = new Client with Master {
 *   override val masterKey = "myMasterKey"
 * }
 * }}}
 *
 * @throws MissingCredential if a master key is not configured.
 *
 * @see [[https://keen.io/docs/security/]]
 */
trait Master extends Reader with Writer {
  self: Client =>

  /**
   * A master key required to make API calls of administrative nature.
   */
  val masterKey: String = settings.masterKey.getOrElse(throw MissingCredential("Master key required for Master"))

  // Since a master key can perform any API call, override read and write keys
  // so that a client can extend only the Master trait when needed.
  override val readKey: String = masterKey
  override val writeKey: String = masterKey

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
   */
  def getEvents: Future[Response] = {
    val path = Seq(version, "projects", projectId, "events").mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns available schema information for this event collection, including properties and their type. It also returns links to sub-resources. See [[https://keen.io/docs/api/reference/#event-collection-resource Event Collection Resource]].
   *
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
}
