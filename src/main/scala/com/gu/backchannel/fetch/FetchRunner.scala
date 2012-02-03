package com.gu.backchannel.fetch

import akka.actor.Actor._
import com.gu.backchannel.model.{Mongo, Update}
import akka.actor.{ActorRef, Actor}
import net.liftweb.json.JsonParser._
import org.joda.time.format.ISODateTimeFormat
import java.text.SimpleDateFormat
import org.joda.time.DateTime

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

  implicit val formats = net.liftweb.json.DefaultFormats

  var lastId: Option[String] = None

  val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")

  def receive = {
    case "fetch" => {
      println("twitter actor pinged " + eventId + " " + hashTag)
      val basicParams: Map[String, String] = Map("q" -> hashTag, "result_type" -> "recent")
      val params = lastId match {
        case None => basicParams
        case Some(s) => basicParams + ("since_id" -> s)
      }
      val response = HttpClient.get("http://search.twitter.com/search.json", params)

      val twitterResponse = parse(response).extract[TwitterResponse]

      twitterResponse.results.foreach { tweet =>
        //val tweetTime = dateFormat.parse(tweet.created_at)
        val tweetTime =  new DateTime().getMillis
        val update = Update(
          `type` = "tweet",
          updateTime = tweetTime,
          updateHtml = "<h3>"+ tweet.from_user + "</h3><p>" + tweet.text + "</p>"
        )

        Mongo.addUpdate(eventId, update)
      }
      lastId = Some(twitterResponse.max_id_str)
    }
  }
}

case class TwitterResponse(max_id_str: String, results: List[Tweet])
case class Tweet(created_at: String, from_user: String, text: String)

class LiveblogFetcherActor(eventId: String, urlslug: String) extends Actor {

  implicit val formats = net.liftweb.json.DefaultFormats

  var lastId: Option[String] = None

  def receive = {
    case "fetch" => {
      println("liveblog actor pinged " + eventId + " " + urlslug)

      val params: Map[String, String] = lastId match {
        case None => Map()
        case Some(s) => Map("offset" -> s)
      }

      val response = HttpClient.get("http://flxapi.gucode.gnl:8080/api/live/" + urlslug, params)
      val liveBlogResponse = parse(response).extract[LiveBlogWrapper].content

      liveBlogResponse.blocks.foreach { block =>
        // TODO sort properly val blocktime = block.publishedDate
        val blocktime = new DateTime().getMillis
        val text = block.elements.map { element =>
          element.fields.text
        }.mkString("")

        val update = Update(
          `type` = "liveblog",
          updateTime = blocktime,
          updateHtml = text
        )
        Mongo.addUpdate(eventId, update)
      }

      val topBlock = liveBlogResponse.blocks.headOption
      topBlock.foreach {block =>
        lastId = Some(block.id)
      }
    }
  }
}

case class LiveBlogWrapper(content: LiveBlogResponse)
case class LiveBlogResponse(blocks: List[LiveBlogBlock])
case class LiveBlogBlock(id: String, publishedDate: Long, elements: List[LiveBlogElement])
case class LiveBlogElement(fields: TextField)
case class TextField(text: String)

class DiscussionFetcherActor(eventId: String, urlslug: String) extends Actor {

  def receive = {
    case "fetch" => { println("discussion actor pinged " + eventId + " " + urlslug) }
  }
}

