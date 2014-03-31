package test

import dispatch.Req
import org.specs2.mutable.Specification
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await,Future,Promise}
import scala.concurrent.duration._
import io.keen.client.scala._

class ClientSpec extends Specification {

  class OkHttpAdapter extends HttpAdapter {

    var lastReq: Option[Req] = None
    var lastKey: Option[String] = None

    override def doRequest(req: Req, key: String): Future[Response] = {
      lastReq = Some(req)
      lastKey = Some(key)
      val p = Promise[Response]()
      Future {
        p.success(Response(200, "Ok"))
      }
      p.future
    }

    def getKey = lastKey

    def getReq = lastReq
  }

  class NokHttpAdapter extends HttpAdapter {

    override def doRequest(req: Req, key: String): Future[Response] = {
      val p = Promise[Response]()
      Future {
        p.success(Response(500, "Internal Server Error"))
      }
      p.future
    }
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
      adapter.getReq.get.url must beEqualTo("https://api.keen.io/3.0/projects")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get project" in {
      val res = Await.result(client.getProject, Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getReq.get.url must beEqualTo("https://api.keen.io/3.0/projects/abc")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get event" in {
      val res = Await.result(client.getEvents, Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getReq.get.url must beEqualTo("https://api.keen.io/3.0/projects/abc/events")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get property" in {
      val res = Await.result(client.getProperty("foo", "bar"), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getReq.get.url must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo/properties/bar")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get collection" in {
      val res = Await.result(client.getCollection("foo"), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getReq.get.url must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle get collection (encoding)" in {
      val res = Await.result(client.getCollection("foo foo"), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getReq.get.url must beEqualTo("https://api.keen.io/3.0/projects/abc/events/foo%20foo")
      adapter.getKey.get must beEqualTo("masterKey")
    }

    "handle count query" in {
      val res = Await.result(client.count("foo"), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      adapter.getReq.get.url must beEqualTo("https://api.keen.io/3.0/projects/abc/queries/count?event_collection=foo")

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
        timeframe = Some("this_week"),
        timezone = Some("America/Chicago"),
        groupBy = Some("foo.name")
      ), Duration(5, "second"))

      res.statusCode must beEqualTo(200)
      var url = adapter.getReq.get.url

      // Test for these with contains because who knows what order they'll be in
      url must contain("https://api.keen.io/3.0/projects/abc/queries/average?")
      url must contain("event_collection=foo%20foo%E2%9D%96")
      url must contain("target_property=bar")
      url must contain("filters=%5B%7B%22property_name%22%3A%20%22baz%22%2C%22operator%22%3A%22eq%22%2C%22property_value%22%3A%22gorch%22%7D%5D")
      url must contain("timeframe=this_week")
      url must contain("timezone=America%2FChicago")
      url must contain("group_by=foo.name")

      adapter.getKey.get must beEqualTo("readKey")
    }

    "shutdown" in {
      Client.shutdown
      1 must beEqualTo(1)
    }
  }

  "Client failures" should {

    val adapter = new NokHttpAdapter()
    val client = new Client(
      projectId = "abc",
      masterKey = "masterKey",
      writeKey = "writeKey",
      readKey = "readKey",
      httpAdapter = adapter
    )

    "handle 500" in {
      val res = Await.result(client.getProjects, Duration(5, "second"))

      res.statusCode must beEqualTo(500)
    }
  }
}