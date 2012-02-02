package com.gu.backchannel

import model.{Mongo, Event}
import org.scalatra.ScalatraFilter
import org.slf4j.{LoggerFactory, Logger}
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.DefaultFormats
import org.scalatra.scalate.ScalateSupport

class PublicActions extends ScalatraFilter with ScalateSupport {

  protected val log = LoggerFactory.getLogger(getClass)
  implicit val formats = DefaultFormats

  //before() { contentType = "text/html" }

  get("/admin/new") {
    contentType = "text/html"
    val event = Event("", "headline", None, None, None, Nil)
    layoutTemplate("/WEB-INF/scalate/templates/edit.ssp", "event" -> event, "isNew" -> true)
  }

  get("/admin/edit/*") {
    contentType = "text/html"
    val id = multiParams("splat").headOption getOrElse halt(status = 400, reason="no id provided")
    val event = Mongo.loadEvent(id) getOrElse halt(status = 404)
    layoutTemplate("/WEB-INF/scalate/templates/edit.ssp", "event" -> event, "isNew" -> false)
  }

  post("/admin/save") {
    val id = params("idInput")
    val headline = params("headlineInput")
    val trailText = params.get("descInput")
    val imageUrl = params.get("imageInput")

    val event = Event(id, headline, trailText, imageUrl, None, Nil)

    Mongo.insert(event)

    redirect("/admin/edit/" + id)
  }

  post("/admin/save/*") {
    val id = multiParams("splat").headOption getOrElse halt(status=400, reason="no id provided")
    val headline = params("headlineInput")
    val trailText = params.get("descInput")
    val imageUrl = params.get("imageInput")

    val currentEvent = Mongo.loadEvent(id) getOrElse halt(status=400, reason="failed to load event")
    val event = currentEvent.copy(headline = headline,
      description = trailText,
      imageUrl = imageUrl)

    Mongo.update(event)

    redirect("/admin/edit/" + id)
  }

  get("/admin/events") {
    contentType = "text/html"
    val events = Mongo.loadAllEvents
    layoutTemplate("/WEB-INF/scalate/templates/list.ssp", "events" -> events)
  }

  error { case e => {
      log.error(e.toString)
      val stackTrace = e.getStackTraceString.split("\n") map { "\tat " + _ } mkString "\n"
      e.toString + "\n" + stackTrace
    }
  }


}
