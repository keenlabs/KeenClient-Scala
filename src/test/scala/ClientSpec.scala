package test

import io.keen.client.scala._
import java.nio.charset.StandardCharsets
import org.specs2.mutable.Specification
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await,Future,Promise}
import spray.http.Uri
import spray.http.Uri._

class ClientSpec extends Specification {

  class OkHttpAdapter extends HttpAdapter {

    var lastUrl: Option[String] = None
    var lastKey: Option[String] = None

    override def doRequest(
      url: String,
      method: String,
      key: String,
      body: Option[String] = None,
      params: Map[String,Option[String]] = Map.empty): Future[Response] = {

      val filteredParams = params.filter(
        // Filter out keys that are None
        _._2.isDefined
      ).map(
        // Convert the remaining tuples to str,str
        param => (param._1 -> param._2.get)
      )
      // Make a Uri
      val finalUrl = Uri(input = url, StandardCharsets.UTF_8, Uri.ParsingMode.Relaxed).withQuery(Query(filteredParams))

      lastUrl = Some(finalUrl.toString)
      lastKey = Some(key)
      val p = Promise[Response]()
      Future {
        p.success(Response(200, "Ok"))
      }
      p.future
    }

    def getKey = lastKey

    def getUrl = lastUrl
  }

  // Sequential because it's less work to share the client instance
  sequential

  "Client" should {

    val adapter = new OkHttpAdapter()
    val client = new Client(
      projectId = "abc",
      masterKey = "masterKey",
      writeKey = "writeKey",
      readKey = "readKey",
      httpAdapter = adapter
    )

    "handle 200" in {
      val res = Await.result(client.getProjects, Duration(5, "second"))

      res.statusCode must beEqualTo(200)
    }

    "handle get projects" in {
      val res = Await.result(client.getProjects, Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get project" in {
      val res = Await.result(client.getProject, Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get event" in {
      val res = Await.result(client.getEvents, Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/events")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get property" in {
      val res = Await.result(client.getProperty("foo", "bar"), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo/properties/bar")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get collection" in {
      val res = Await.result(client.getCollection("foo"), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get collection (encoding)" in {
      val res = Await.result(client.getCollection("foo foo"), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo+foo")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle count query" in {
      val res = Await.result(client.count("foo"), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getUrl.get must beEqualTo("https://api.keen.io/3.0/projects/abc/queries/count?event_collection=foo")

      adapter.getKey.get must beEqualTo("readKey")
    }

    // We'll test average thoroughly but since all the query method use the same underlying
    // implementation to build URLs we can skip that on the next ones. We'll get nasty in
    // here with encodings and such.
    "handle average query" in {
      val res = Await.result(client.average(
        collection = "foo foo❖",
        targetProperty = "bar",
        filters = Some("""[{"property_name": "baz","operator":"eq","property_value":"gorch"}]"""),
        timeframe = Some("this_week "),
        timezone = Some("America/Chicago"),
        groupBy = Some("foo.name")
      ), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      var url = adapter.getUrl.get

      // Test for these with contains because who knows what order they'll be in
      url must contain("https://api.keen.io/3.0/projects/abc/queries/average?")
      url must contain("event_collection=foo+foo%E2%9D%96")
      url must contain("target_property=bar")
      url must contain("filters=%5B%7B%22property_name%22%3A%20%22baz%22%2C%22operator%22%3A%22eq%22%2C%22property_value%22%3A%22gorch%22%7D%5D")
      url must contain("timeframe=this_week")
      url must contain("timezone=America%2FChicago")
      url must contain("group_by=foo.name")

      adapter.getKey.get must beEqualTo("readKey")
    }

    "shutdown" in {
      client.shutdown
      1 must beEqualTo(1)
    }
  }
}