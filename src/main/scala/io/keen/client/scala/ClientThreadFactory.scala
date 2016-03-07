package io.keen.client.scala

import java.util.concurrent.ThreadFactory

class ClientThreadFactory extends ThreadFactory {
  /**
   * Returns a new daemon thread.
   *
   * @param  r Class to be executed by the thread.
   * @return   A new damon thread.
   */
  def newThread(r: Runnable): Thread = {
    val t: Thread = new Thread(r)
    t.setDaemon(true)
    t
  }
}
