package com.gu.backchannel

import org.scalatra.ScalatraFilter
import org.slf4j.{LoggerFactory, Logger}
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.DefaultFormats
import org.scalatra.scalate.ScalateSupport

class PublicActions extends ScalatraFilter with ScalateSupport {

  protected val log = LoggerFactory.getLogger(getClass)
  implicit val formats = DefaultFormats

  before() { contentType = "text/html" }

  get("/admin/hello") {
   layoutTemplate("templates/hello.ssp")
  }

  error { case e => {
      log.error(e.toString)
      val stackTrace = e.getStackTraceString.split("\n") map { "\tat " + _ } mkString "\n"
      e.toString + "\n" + stackTrace
    }
  }


}
