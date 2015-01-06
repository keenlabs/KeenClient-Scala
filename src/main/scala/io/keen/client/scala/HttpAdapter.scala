package io.keen.client.scala

import scala.concurrent.Future

case class Response(statusCode: Int, body: String)

trait HttpAdapterComponent {
  val httpAdapter: HttpAdapter
}

trait HttpAdapter {
  def doRequest(
    scheme: String,
    authority: String,
    path: String,
    method: String,
    key: String,
    body: Option[String] = None,
    params: Map[String, Option[String]] = Map.empty
  ): Future[Response]

  def shutdown(): Unit
}
