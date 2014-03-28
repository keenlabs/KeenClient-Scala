package test

import org.specs2.mutable.Specification
import scala.concurrent.Await
import scala.concurrent.duration._
import io.keen.client.scala.Client

class ClientIntegrationSpec extends Specification {

  // args(exclude = "integration")

  sequential

  "Client" should {

    lazy val client = new Client(
      projectId = sys.env("KEEN_PROJECT_ID"),
      masterKey = sys.env("KEEN_MASTER_KEY"),
      writeKey = sys.env("KEEN_WRITE_KEY"),
      readKey = sys.env("KEEN_READ_KEY")
    )

    // "fetch collection" in {
    //   val res = Await.result(client.getCollection(
    //     collection = "foo"
    //   ), Duration(5, "second"))
    //   println(res.body)
    //   res.statusCode must beEqualTo(200)
    // }

    // "fetch projects" in {
    //   val res = Await.result(client.getProjects, Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "fetch project" in {
    //   val res = Await.result(client.getProject, Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "fetch property" in {
    //   val res = Await.result(client.getProperty(
    //     collection = "foo",
    //     name = "foo"
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "fetch event collection" in {

    //   val res = Await.result(client.getEvents, Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    "write an event" in {

      val res = Await.result(client.addEvent(
        collection = "foo",
        event = """{"foo": "bar"}"""
      ), Duration(5, "second"))
      println(res.body)
      res.statusCode must beEqualTo(201)
    }

    // "fetch queries" in {
    //   val res = Await.result(client.getQueries, Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "count" in {
    //   val res = Await.result(client.count(
    //     collection = "foo"
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "count with filters and timeframe" in {
    //   val res = Await.result(client.count(
    //     collection = "foo",
    //     filters = Some("""[{"property_name": "baz","operator":"eq","property_value":"gorch"}]"""),
    //     timeframe = Some("this_week")
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "count unique" in {
    //   val res = Await.result(client.countUnique(
    //     collection = "foo",
    //     targetProperty = "gorch"
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "minimum" in {
    //   val res = Await.result(client.minimum(
    //     collection = "foo",
    //     targetProperty = "gorch"
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "maximum" in {
    //   val res = Await.result(client.maximum(
    //     collection = "foo",
    //     targetProperty = "gorch"
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "average" in {
    //   val res = Await.result(client.average(
    //     collection = "foo",
    //     targetProperty = "gorch"
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "sum" in {
    //   val res = Await.result(client.sum(
    //     collection = "foo",
    //     targetProperty = "gorch"
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // "select unique" in {
    //   val res = Await.result(client.selectUnique(
    //     collection = "foo",
    //     targetProperty = "gorch"
    //   ), Duration(5, "second"))
    //   // println(res.getResponseBody)
    //   res.statusCode must beEqualTo(200)
    // }

    // // // Is this working?
    // // "delete property" in {
    // //   val res = Await.result(client.deleteProperty(
    // //     collection = "foo",
    // //     name = "foo"
    // //   ), Duration(5, "second"))
    // //   println(res.getResponseBody)
    // //   res.statusCode must beEqualTo(204)
    // // }

    // // "write many events" in {

    // //   val res = Await.result(client.addEvents(
    // //     events = """{"foo": [{"foo": "bar"},{"baz": "gorch"}], "bar": [{"hood":"winked"}]}"""
    // //   ), Duration(5, "second"))
    // //   // println(res.getResponseBody)
    // //   // Not working!! XXX
    // //   res.statusCode must beEqualTo(500)
    // // }

    "shutdown" in {
      client.shutdown
      1 must beEqualTo(1)
    }
  } section("integration")
}