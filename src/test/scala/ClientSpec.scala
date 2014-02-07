package test

import dispatch.Req
import org.specs2.mutable._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await,Future,Promise}
import scala.concurrent.duration._
import io.keen.client.scala._

class ClientSpec extends Specification {

  class MissingHttpAdapter extends HttpAdapter {
    override def doRequest(req: Req, key: String): Future[Response] = {
      val p = Promise[Response]()
      Future {
        p.success(Response(404, "It's missing!"))
      }
      p.future
    }
  }

  "Client" should {

    "handle 404" in {
      val client = new Client(
        projectId = "abc",
        masterKey = "def",
        writeKey = "ghi",
        readKey = "jkl",
        httpAdapter = new MissingHttpAdapter()
      )
      val res = Await.result(client.getProjects, Duration(5, "second"))

      res.statusCode must beEqualTo(404)
    }
   "shutdown" in {
      Client.shutdown
      1 must beEqualTo(1)
    }
  }
}