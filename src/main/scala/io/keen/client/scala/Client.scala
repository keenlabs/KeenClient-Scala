package io.keen.client.scala

import scala.concurrent.Future

import com.typesafe.config.{ Config, ConfigFactory }
import grizzled.slf4j.Logging

// XXX Remaining: Funnel, Saved Queries List, Saved Queries Row, Saved Queries Row Result
// Event deletion

/**
 * A keen.io API client. A plain `Client` instance doesn't do a whole lot—you'll
 * want to mix in [[AccessLevel]]s like [[Writer]], [[Reader]], or [[Master]]
 * depending on the API operations that you need to perform.
 *
 * The client defaults to using [[HttpAdapterSpray a built-in adapter for the
 * Spray HTTP toolkit]] for HTTP requests. Another HTTP client library can be
 * plugged in by implementing the [[HttpAdapter]] interface.
 *
 * @param config Client configuration, by default loaded from `application.conf`.
 *
 * @example Overriding the default [[HttpAdapter]]
 * {{{
 * val keen = new Client {
 *   override val httpAdapter = new HttpAdapterDispatch
 * }
 * }}}
 *
 * @see [[https://keen.io/docs/api/ The Keen IO API Reference]]
 */
class Client(
    config: Config = ConfigFactory.load(),
    // These will move to a config file:
    val scheme: String = "https",
    val authority: String = "api.keen.io",
    val version: String = "3.0"
) extends HttpAdapterComponent with Logging {

  /** The concrete [[HttpAdapter]] used to make API requests. */
  val httpAdapter: HttpAdapter = new HttpAdapterSpray

  /** The client's configuration settings. */
  val settings: Settings = new Settings(config)

  // TODO: This is a smell, see http://12factor.net/config
  val environment: Option[String] = settings.environment

  /**
   * Shuts down the client's [[HttpAdapter]], to perform cleanup such as closing
   * connections.
   */
  def shutdown(): Unit = httpAdapter.shutdown()
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

  /**
   * Project identifier for the Keen project that an `AccessLevel`'s API key is
   * associated with.
   */
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
    params: Map[String, Option[String]] = Map.empty
  ) = {
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
   * Returns the average across all numeric values for the target property in the event collection matching the given criteria.
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   *
   * @see [[https://keen.io/docs/api/#average Average API Reference]]
   */
  def average(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String] = None
  ): Future[Response] =

    doQuery(
      analysisType = "average",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy
    )

  /**
   * Returns the number of resources in the event collection matching the given criteria.
   *
   * @param collection The name of the event collection you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   *
   * @see [[https://keen.io/docs/api/#count Count API Reference]]
   */
  def count(
    collection: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String] = None
  ): Future[Response] =

    doQuery(
      analysisType = "count",
      collection = collection,
      targetProperty = None,
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy
    )

  /**
   * Returns the number of '''unique''' resources in the event collection matching the given criteria.
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   *
   * @see [[https://keen.io/docs/api/#count-unique Count Unique API Reference]]
   */
  def countUnique(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String] = None
  ): Future[Response] =

    doQuery(
      analysisType = "count",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy
    )

  /**
   * Creates an extraction request for full-form event data with all property values.
   *
   * If the `email` parameter is given, the extraction will be processed asynchronously
   * and an email will be sent to the specified address when complete. Otherwise events
   * are returned in a synchronous JSON response with a limit of 100,000 events.
   *
   * @param collection The name of the event collection you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request
   *   based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window
   *   of time. If no timeframe is specified, all events will be counted. See
   *   [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param email Email address to notify when asynchronous extraction is ready for download.
   * @param latest An integer containing the number of most recent events to extract.
   * @param propertyNames An array of strings containing properties you wish to extract.
   *   If this parameter is omitted, all properties will be returned.
   *
   * @see [[https://keen.io/docs/api/#extractions Extractions API Reference]]
   * @todo Should accept timezone parameter: [[https://github.com/keenlabs/KeenClient-Scala/issues/37]]
   */
  def extraction(
    collection: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    email: Option[String] = None,
    latest: Option[String] = None, // TODO: Integer
    propertyNames: Option[String] = None // TODO: List of Strings
  ): Future[Response] =

    doQuery(
      analysisType = "extraction",
      collection = collection,
      filters = filters,
      timeframe = timeframe,
      email = email,
      latest = latest,
      propertyNames = propertyNames
    )

  /**
   * Returns the maximum numeric value for the target property in the event collection matching the given criteria.
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   *
   * @see [[https://keen.io/docs/api/#maximum Maximum API Reference]]
   */
  def maximum(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String] = None
  ): Future[Response] =

    doQuery(
      analysisType = "maximum",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy
    )

  /**
   * Returns the minimum numeric value for the target property in the event
   * collection matching the given criteria.
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   *
   * @see [[https://keen.io/docs/api/#minimum Minimum API Reference]]
   */
  def minimum(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String] = None
  ): Future[Response] =

    doQuery(
      analysisType = "minimum",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy
    )

  /**
   * Returns a list of '''unique''' resources in the event collection matching the given criteria.
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   *
   * @see [[https://keen.io/docs/api/#select-unique Select Unique API Reference]]
   */
  def selectUnique(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String] = None
  ): Future[Response] =

    doQuery(
      analysisType = "select_unique",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy
    )

  /**
   * Returns the sum across all numeric values for the target property in the
   * event collection matching the given criteria.
   *
   * @param collection The name of the event collection you are analyzing.
   * @param targetProperty The name of the property you are analyzing.
   * @param filters Filters are used to narrow down the events used in an analysis request based on event property values. See [[https://keen.io/docs/data-analysis/filters/ Filters]].
   * @param timeframe A Timeframe specifies the events to use for analysis based on a window of time. If no timeframe is specified, all events will be counted. See [[https://keen.io/docs/data-analysis/timeframe/ Timeframes]].
   * @param timezone Modifies the timeframe filters for Relative Timeframes to match a specific timezone.
   * @param groupBy The group_by parameter specifies the name of a property by which you would like to group the results. Using this parameter changes the response format. See [[https://keen.io/docs/data-analysis/group-by/ Group By]].
   *
   * @see [[https://keen.io/docs/api/#sum Sum API Reference]]
   */
  def sum(
    collection: String,
    targetProperty: String,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String] = None
  ): Future[Response] =

    doQuery(
      analysisType = "sum",
      collection = collection,
      targetProperty = Some(targetProperty),
      filters = filters,
      timeframe = timeframe,
      timezone = timezone,
      groupBy = groupBy
    )

  private def doQuery(
    analysisType: String,
    collection: String,
    targetProperty: Option[String] = None,
    filters: Option[String] = None,
    timeframe: Option[String] = None,
    timezone: Option[String] = None,
    groupBy: Option[String] = None,
    email: Option[String] = None,
    latest: Option[String] = None,
    propertyNames: Option[String] = None
  ): Future[Response] = {

    val path = Seq(version, "projects", projectId, "queries", analysisType).mkString("/")

    val params = Map(
      "event_collection" -> Some(collection),
      "target_property" -> targetProperty,
      "filters" -> filters,
      "timeframe" -> timeframe,
      "timezone" -> timezone,
      "group_by" -> groupBy,
      "email" -> email,
      "latest" -> latest,
      "property_names" -> propertyNames
    )

    // TODO: Prefer POST when available, avoids URL encoding concerns, etc.
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
   * Publish a single event.
   *
   * @param collection The collection to which the event will be added.
   * @param event The event
   *
   * @see [[https://keen.io/docs/api/#record-a-single-event Record a single event API Reference]]
   */
  def addEvent(collection: String, event: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection).mkString("/")
    doRequest(path = path, method = "POST", key = writeKey, body = Some(event))
  }

  /**
   * Publish multiple events.
   *
   * @param events The events to add to the project.
   * @see [[https://keen.io/docs/api/#record-multiple-events Record multiple events API Reference]]
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
   * Deletes the entire event collection. This is irreversible and will only work
   * for collections under 10k events.
   *
   * @param collection The name of the collection.
   * @see [[https://keen.io/docs/api/#delete-a-collection Delete a Collection API Reference]]
   */
  def deleteCollection(collection: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection).mkString("/")
    doRequest(path = path, method = "DELETE", key = masterKey)
  }

  /**
   * Removes a property and deletes all values stored with that property name.
   * @see [[https://keen.io/docs/api/#delete-a-property Delete a Property API Reference]]
   */
  def deleteProperty(collection: String, name: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection, "properties", name).mkString("/")
    doRequest(path = path, method = "DELETE", key = masterKey)
  }

  /**
   * Returns schema information for all the event collections in this project.
   * @see [[https://keen.io/docs/api/#inspect-all-collections Inspect all collections API Reference]]
   * @todo This only requires a read key, move to Reader
   */
  def getEvents: Future[Response] = {
    val path = Seq(version, "projects", projectId, "events").mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns available schema information for this event collection, including
   * properties and their type. It also returns links to sub-resources.
   *
   * @param collection The name of the collection.
   * @see [[https://keen.io/docs/api/#inspect-a-single-collection Inspect a single collection API Reference]]
   * @todo This only requires a read key, move to Reader
   */
  def getCollection(collection: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection).mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns the projects accessible to the API user, as well as links to project sub-resources for
   * discovery.
   * @see [[https://keen.io/docs/api/#inspect-all-projects Inspect all projects API Reference]]
   */
  def getProjects: Future[Response] = {
    val path = Seq(version, "projects").mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns detailed information about the specific project, as well as links to related resources.
   * @see [[https://keen.io/docs/api/#inspect-a-single-project Inspect a single project API Reference]]
   */
  def getProject: Future[Response] = {
    val path = Seq(version, "projects", projectId).mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns the property name, type, and a link to sub-resources.
   * @see [[https://keen.io/docs/api/#inspect-a-single-property Inspect a single property API Reference]]
   * @todo This only requires a read key, move to Reader
   */
  def getProperty(collection: String, name: String): Future[Response] = {
    val path = Seq(version, "projects", projectId, "events", collection, "properties", name).mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }

  /**
   * Returns the list of available query resources as paths to their endpoints.
   * @see [[https://keen.io/docs/api/#query-availability Query Availability API Reference]]
   */
  def getQueries: Future[Response] = {
    val path = Seq(version, "projects", projectId, "queries").mkString("/")
    doRequest(path = path, method = "GET", key = masterKey)
  }
}
