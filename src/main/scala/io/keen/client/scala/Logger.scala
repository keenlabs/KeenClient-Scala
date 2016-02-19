package io.keen.client.scala

import grizzled.slf4j.Logging

trait Logger extends Logging {

  var loggingEnabled: Boolean = false

  /**
   * Disables logging
   */
  def disableLogging() = {
    loggingEnabled = false
  }

  /**
   * Enables logging
   */
  def enableLogging() = {
    loggingEnabled = true
  }

  def kdebug(message: String): Unit = {
    klog("debug", message)
  }

  def kerror(message: String): Unit = {
    klog("error", message)
  }

  def kinfo(message: String): Unit = {
    klog("info", message)
  }

  def ktrace(message: String): Unit = {
    klog("trace", message)
  }

  def kwarn(message: String): Unit = {
    klog("warn", message)
  }

  def klog(level: String, message: String): Unit = {
    // only log if logging has been enabled
    if(loggingEnabled) {
      level.toLowerCase match {
        case "debug" => debug(message)
        case "error" => error(message)
        case "info" => info(message)
        case "trace" => trace(message)
        case "warn" => warn(message)
      }  
    }
  }

}