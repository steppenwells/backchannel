package com.gu.backchannel

import fetch.{StopRecordMessage, RecordMessage, FetchActor}
import model.{TimedUpdate, Mongo, Event}
import org.scalatra.ScalatraFilter
import org.slf4j.{LoggerFactory, Logger}
import net.liftweb.json._
import net.liftweb.json.Extraction._
import net.liftweb.json.DefaultFormats
import org.scalatra.scalate.ScalateSupport
import akka.actor.Actor._
import akka.actor.Scheduler
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime

class PublicActions extends ScalatraFilter with ScalateSupport {

  protected val log = LoggerFactory.getLogger(getClass)
  implicit val formats = DefaultFormats

  val fetcher = actorOf(new FetchActor()).start()

  Scheduler.schedule(fetcher, "fetch", 15, 15, TimeUnit.SECONDS)

  //before() { contentType = "text/html" }

  get("/admin/new") {
    contentType = "text/html"
    val event = Event("", "headline", None, None, None, Nil, Map())
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

    val twitterPair = params.get("twitterInput") flatMap ( fetcherPair("twitter", _))
    val lbPair = params.get("liveblogInput") flatMap ( fetcherPair("liveblog", _))
    val discussionPair = params.get("discussionInput") flatMap ( fetcherPair("discussion", _))

    val updaters = List(twitterPair, lbPair, discussionPair) flatMap(p => p) toMap

    val event = Event(id, headline, trailText, imageUrl, None, Nil, updaters)

    Mongo.insert(event)

    redirect("/admin/edit/" + id)
  }

  def fetcherPair(name: String, data: String) = {
    data.trim match {
      case "" => None
      case s => Some(name -> s)
    }
  }

  post("/admin/save/*") {
    val id = multiParams("splat").headOption getOrElse halt(status=400, reason="no id provided")
    val headline = params("headlineInput")
    val trailText = params.get("descInput")
    val imageUrl = params.get("imageInput")

    val fudgetime = params.getOrElse("startTimeInput", "")


    val twitterPair = params.get("twitterInput") flatMap ( fetcherPair("twitter", _))
    val lbPair = params.get("liveblogInput") flatMap ( fetcherPair("liveblog", _))
    val discussionPair = params.get("discussionInput") flatMap ( fetcherPair("discussion", _))

    val updaters = List(twitterPair, lbPair, discussionPair) flatMap(p => p) toMap

    val currentEvent = Mongo.loadEvent(id) getOrElse halt(status=400, reason="failed to load event")

    val fudgedTime = fudgetime.trim match {
      case "" => currentEvent.startTime
      case s => Some(s.toLong)
    }

    val event = currentEvent.copy(headline = headline,
      description = trailText,
      imageUrl = imageUrl,
      updateFetchers = updaters,
      startTime = fudgedTime
    )

    Mongo.update(event)

    redirect("/admin/edit/" + id)
  }

  get("/admin/record/*") {
    val id = multiParams("splat").headOption getOrElse halt(status=400, reason="no id provided")
    val currentEvent = Mongo.loadEvent(id) getOrElse halt(status=400, reason="failed to load event")

    currentEvent.startTime match {
      case Some(t) => //nothing
      case None => {
        val event = currentEvent.copy(startTime = Some(new DateTime().getMillis()))
        Mongo.update(event)
      }
    }

    fetcher ! RecordMessage(id)

    redirect("/admin/events")
  }

  get("/admin/stop/*") {
    val id = multiParams("splat").headOption getOrElse halt(status=400, reason="no id provided")

    fetcher ! StopRecordMessage(id)

    redirect("/admin/events")
  }

  get("/admin/events") {
    contentType = "text/html"
    val events = Mongo.loadAllEvents
    layoutTemplate("/WEB-INF/scalate/templates/list.ssp", "events" -> events)
  }

  get("/frontend/live/*") {
    contentType = "text/html"
    val id = multiParams("splat").headOption getOrElse halt(status=400, reason="no id provided")
    val event = Mongo.loadEvent(id) getOrElse halt(status=400, reason="failed to load event")

    val updates = event.updates groupBy(_.`type`)
    val timedUpdate = TimedUpdate(new DateTime().getMillis().toString, updates, event.updates.length)

    val updateJson = pretty(render(decompose(timedUpdate)))

    layoutTemplate("/WEB-INF/scalate/templates/magicView.ssp",
      "updateJson" -> updateJson,
      "eventId" -> id,
      "latestTime" -> timedUpdate.time,
      "isPlayback" -> false,
      "event" -> event)
  }
  
  get("/frontend/playback/*") {
      contentType = "text/html"
      val id = multiParams("splat").headOption getOrElse halt(status=400, reason="no id provided")
      val event = Mongo.loadEvent(id) getOrElse halt(status=400, reason="failed to load event")
  
      val startTime = event.startTime.getOrElse(new DateTime().getMillis())
      val updates = event.updates.filter(_.updateTime < startTime) groupBy(_.`type`)
      val timedUpdate = TimedUpdate(startTime.toString, updates, event.updates.filter(_.updateTime < startTime).length)
  
      val updateJson = pretty(render(decompose(timedUpdate)))
  
      layoutTemplate("/WEB-INF/scalate/templates/magicView.ssp",
        "updateJson" -> updateJson,
        "eventId" -> id,
        "latestTime" -> timedUpdate.time,
        "isPlayback" -> true,
        "event" -> event)
    }

  get("/frontend/updates/*") {
    contentType = "application/json"

    val id = multiParams("splat").headOption getOrElse halt(status=400, reason="no id provided")
    val event = Mongo.loadEvent(id) getOrElse halt(status=400, reason="failed to load event")

    val from = params.get("from").map(_.toLong)
    val to = params.get("to").map(_.toLong)
    val updatesFrom = from match {
      case None => event.updates
      case Some(f) => event.updates.filter(_.updateTime >= f)
    }
    val updatesInPeriod = to match {
      case None => updatesFrom
      case Some(f) => updatesFrom.filter(_.updateTime < f)
    }

    val updates = updatesInPeriod groupBy(_.`type`)

    val timedUpdate = TimedUpdate(new DateTime().getMillis().toString, updates, updatesInPeriod.length)

    pretty(render(decompose(timedUpdate)))
  }

  error { case e => {
      log.error(e.toString)
      val stackTrace = e.getStackTraceString.split("\n") map { "\tat " + _ } mkString "\n"
      e.toString + "\n" + stackTrace
    }
  }
}
