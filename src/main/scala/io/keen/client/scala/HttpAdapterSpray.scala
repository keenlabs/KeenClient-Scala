package io.keen.client.scala

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import grizzled.slf4j.Logging
import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import spray.can.Http
import spray.http._
import spray.http.Uri._
import spray.http.HttpHeaders.RawHeader
import spray.httpx.RequestBuilding._

/**
 * An [[HttpAdapter]] built on HTTP client support of the [[http://spray.io/
 * Spray HTTP toolkit]].
 *
 * == Akka and Actor Systems ==
 *
 * Spray is built upon $AkkaIO and uses an Akka $ActorSystem to handle HTTP I/O.
 * By default, `HttpAdapterSpray` creates an `ActorSystem` for its own use so
 * that it works out of the box. If you are using it within an Akka application,
 * however, it is possible to specify the `ActorSystem` within which it will run
 * using an implicit parameter.
 *
 * @example Using a custom $ActorSystem
 * {{{
 * val adapter = new HttpAdapterSpray()(ActorSystem("myapp-system"))
 * }}}
 *
 * @example Using a custom $ActorSystem, implicitly for a scope
 * {{{
 * implicit val system = ActorSystem("myapp-system")
 * val adapter = new HttpAdapterSpray
 * }}}
 *
 * @param httpTimeoutSeconds Sets a timeout for HTTP requests, in seconds.
 * @todo Move the timeout constructor param to config
 *
 * @define AkkaIO [[http://doc.akka.io/docs/akka/2.3.14/scala/io.html Akka I/O]]
 * @define ActorSystem [[akka.actor.ActorSystem ActorSystem]]
 */
class HttpAdapterSpray(httpTimeoutSeconds: Int = 10)(implicit val actorSystem: ActorSystem = ActorSystem("keen-client"))
    extends HttpAdapter with Logging {

  import actorSystem.dispatcher // execution context for futures

  // Akka's Ask pattern requires an implicit timeout to know
  // how long to wait for a response.
  implicit val timeout = Timeout(httpTimeoutSeconds, TimeUnit.SECONDS)

  def doRequest(
    scheme: String,
    authority: String,
    path: String,
    method: String,
    key: String,
    body: Option[String] = None,
    params: Map[String, Option[String]] = Map.empty
  ): Future[Response] = {

    // Turn a map of string,opt[string] into a map of string,string which is
    // what Query wants
    val filteredParams = params.filter(
      // Filter out keys that are None
      _._2.isDefined
    ).map(
      // Convert the remaining tuples to str,str
      param => (param._1 -> param._2.get)
    )
    // Make a Uri
    val finalUrl = Uri(
      scheme = scheme,
      authority = Authority(host = Host(authority)),
      path = Path("/" + path),
      query = Query(filteredParams)
    )

    // Use the provided case classes from spray-client
    // to construct an HTTP request of the type needed.
    val httpMethod: HttpRequest = method match {
      case "DELETE" => Delete(finalUrl, body)
      case "GET"    => Get(finalUrl, body)
      case "POST"   => Post(finalUrl, HttpEntity(ContentTypes.`application/json`, body.get))
      case _        => throw new IllegalArgumentException("Unknown HTTP method: " + method)
    }

    debug("%s: %s".format(method, finalUrl))
    // For spelunkers, the ? is a function of the Akka "ask pattern". Unlike !
    // it waits for a response in the form of a future. In this case we're
    // sending along a case class representing the type of HTTP request we want
    // to do and something down in the guts of the actors handles it and gets
    // us a response.
    (IO(Http) ? httpMethod.withHeaders(
      RawHeader("Authorization", key)
    ))
      .mapTo[HttpResponse].map({ res =>
        Response(statusCode = res.status.intValue, res.entity.asString)
      })
  }

  /**
   * @inheritdoc
   *
   * Disconnects any remaining connections. Both idle and active. If you are accessing
   * Keen through a proxy that keeps connections alive this is useful.
   *
   * If [[HttpAdapterSpray]]'s default $ActorSystem is in use, it will be shut
   * down; if a custom `ActorSystem` has been supplied, it will not.
   */
  def shutdown() = {
    (IO(Http) ? Http.CloseAll) onComplete {
      // When this completes we will shutdown the actor system if it wasn't
      // supplied by the user.
      case Success(_) => if (actorSystem.name == "keen-client") actorSystem.shutdown()

      // If we fail to close not sure what we can except rethrow
      case Failure(t) => throw t
    }
  }
}
