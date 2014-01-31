package test

import org.specs2.mutable._
import scala.concurrent.Await
import scala.concurrent.duration._
import keen._

class ClientSpec extends Specification {

  sequential

  "Client" should {

    val client = new Client(
      masterKey = sys.env("KEEN_MASTER_KEY"),
      writeKey = sys.env("KEEN_WRITE_KEY"),
      readKey = sys.env("KEEN_READ_KEY")
    )
    println(sys.env("USER").isEmpty)

    "fetch projects" in {

      val res = Await.result(client.projects, Duration(5, "second"))
      // println(res.getResponseBody)
      res.getStatusCode must beEqualTo(200)
    }

    "fetch project" in {

      val res = Await.result(client.project(projectId = sys.env("KEEN_PROJECT_ID")), Duration(5, "second"))
      // println(res.getResponseBody)
      res.getStatusCode must beEqualTo(200)
    }

    "fetch event collection" in {

      val res = Await.result(client.events(projectId = sys.env("KEEN_PROJECT_ID")), Duration(5, "second"))
      // println(res.getResponseBody)
      res.getStatusCode must beEqualTo(200)
    }
  }
}