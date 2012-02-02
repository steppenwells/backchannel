package com.gu.backchannel.fetch

import akka.actor.Actor._
import com.gu.backchannel.model.{Mongo, Update}
import akka.actor.{ActorRef, Actor}

class FetchActor extends Actor {

  var runningFetchers: Map[FetcherId, ActorRef] = Map()

  def receive = {
    case "fetch" => {
      runningFetchers.values.foreach ( _ ! "fetch" )
    }

    case RecordMessage(eventId) => {
      val eventOption = Mongo.loadEvent(eventId)

      eventOption foreach { event =>
        event.updateFetchers.get("twitter") foreach { twitterHash =>
          val a = actorOf(new TwitterFetcherActor(eventId, twitterHash)).start()
          runningFetchers = runningFetchers + (FetcherId(eventId, "twitter") -> a)
        }

        event.updateFetchers.get("liveblog") foreach { urlslug =>
          val a = actorOf(new LiveblogFetcherActor(eventId, urlslug)).start()
          runningFetchers = runningFetchers + (FetcherId(eventId, "liveblog") -> a)
        }

        event.updateFetchers.get("discussion") foreach { urlslug =>
          val a = actorOf(new DiscussionFetcherActor(eventId, urlslug)).start()
          runningFetchers = runningFetchers + (FetcherId(eventId, "discussion") -> a)
        }
      }
    }

    case StopRecordMessage(eventId) => {
      val ta = runningFetchers.get(FetcherId(eventId, "twitter"))
      val la = runningFetchers.get(FetcherId(eventId, "liveblog"))
      val da = runningFetchers.get(FetcherId(eventId, "discussion"))

      ta foreach { f =>
        f.stop()
        runningFetchers = runningFetchers - FetcherId(eventId, "twitter")
      }
      la foreach { f =>
        f.stop()
        runningFetchers = runningFetchers - FetcherId(eventId, "liveblog")
      }
      da foreach { f =>
        f.stop()
        runningFetchers = runningFetchers - FetcherId(eventId, "discussion")
      }

    }
  }
}

case class RecordMessage(eventId: String)
case class StopRecordMessage(eventId: String)

case class FetcherId(eventId: String, `type`: String)


class TwitterFetcherActor(eventId: String, hashTag: String) extends Actor {

  def receive = {
    case "fetch" => { println("twitter actor pinged " + eventId + " " + hashTag) }
  }
}

class LiveblogFetcherActor(eventId: String, urlslug: String) extends Actor {

  def receive = {
    case "fetch" => { println("liveblog actor pinged " + eventId + " " + urlslug) }
  }
}

class DiscussionFetcherActor(eventId: String, urlslug: String) extends Actor {

  def receive = {
    case "fetch" => { println("discussion actor pinged " + eventId + " " + urlslug) }
  }
}

