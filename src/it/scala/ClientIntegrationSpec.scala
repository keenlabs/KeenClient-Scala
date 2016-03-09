package io.keen.client.scala
package test

import scala.concurrent.Await
import scala.concurrent.duration._

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

class ClientIntegrationSpec extends Specification with NoTimeConversions {
  // Timeout used for most future awaits, etc. ScalaTest and Akka TestKit both
  // feature scalable time dilation for testing on CI servers that might be
  // slow--see IntegrationPatience in ScalaTest, not sure if specs2 has similar...
  val timeout = 4.seconds
  val timeframe = Some("this_week") // all queries require timeframes

  sequential

  // This set of expectations currently assumes master access. Should
  // eventually break out some to test access control granularly.
  "Client" should {

    lazy val client = new Client with Master

    lazy val dispatchClient = new Client with Master {
      override val httpAdapter = new HttpAdapterDispatch
    }

    "fetch collection" in {
      val res = Await.result(client.getCollection(
        collection = "foo"
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "fetch collection dispatchClient" in {
      val res = Await.result(dispatchClient.getCollection(
        collection = "foo"
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "fetch projects" in {
      val res = Await.result(client.getProjects, timeout)
      res.statusCode must beEqualTo(200)
    }

    "fetch project" in {
      val res = Await.result(client.getProject, timeout)
      res.statusCode must beEqualTo(200)
    }

    "fetch property" in {
      val res = Await.result(client.getProperty(
        collection = "foo",
        name = "foo"
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "fetch event collection" in {

      val res = Await.result(client.getEvents, timeout)
      res.statusCode must beEqualTo(200)
    }

    "write an event" in {

      val res = Await.result(client.addEvent(
        collection = "foo",
        event = """{"foo": "bar"}"""
      ), timeout)
      res.statusCode must beEqualTo(201)
    }

    "write an event dispatchClient" in {

      val res = Await.result(dispatchClient.addEvent(
        collection = "foo",
        event = """{"foo": "bar"}"""
      ), timeout)
      res.statusCode must beEqualTo(201)
    }

    "fetch queries" in {
      val res = Await.result(client.getQueries, timeout)
      res.statusCode must beEqualTo(200)
    }

    "count" in {
      val res = Await.result(client.count(
        collection = "foo",
        timeframe = timeframe
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "count with filters and timeframe" in {
      val res = Await.result(client.count(
        collection = "foo",
        filters = Some("""[{"property_name": "baz","operator":"eq","property_value":"gorch"}]"""),
        timeframe = timeframe
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "count unique" in {
      val res = Await.result(client.countUnique(
        collection = "foo",
        targetProperty = "gorch",
        timeframe = timeframe
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "minimum" in {
      val res = Await.result(client.minimum(
        collection = "foo",
        targetProperty = "gorch",
        timeframe = timeframe
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "maximum" in {
      val res = Await.result(client.maximum(
        collection = "foo",
        targetProperty = "gorch",
        timeframe = timeframe
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "average" in {
      val res = Await.result(client.average(
        collection = "foo",
        targetProperty = "gorch",
        timeframe = timeframe
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "sum" in {
      val res = Await.result(client.sum(
        collection = "foo",
        targetProperty = "gorch",
        timeframe = timeframe
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "select unique" in {
      val res = Await.result(client.selectUnique(
        collection = "foo",
        targetProperty = "gorch",
        timeframe = timeframe
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    // // Is this working?
    // "delete property" in {
    //   val res = Await.result(client.deleteProperty(
    //     collection = "foo",
    //     name = "foo"
    //   ), timeout)
    //   println(res.getResponseBody)
    //   res.statusCode must beEqualTo(204)
    // }

    "write many events" in {

      val res = Await.result(client.addEvents(
        events = """{"foo": [{"foo": "bar"},{"baz": "gorch"}], "bar": [{"hood":"winked"}]}"""
      ), timeout)
      res.statusCode must beEqualTo(200)
    }

    "shutdown" in {
      client.shutdown()
      1 must beEqualTo(1)
    }
  }
}
