package io.keen.client.scala

import scala.concurrent.Future

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

  def shutdown
}
