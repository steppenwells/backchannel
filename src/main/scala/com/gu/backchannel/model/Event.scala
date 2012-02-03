package com.gu.backchannel.model

import com.novus.salat.annotations._
import org.bson.types.ObjectId


case class Event(@Key("_id") id: String = new ObjectId().toString,
                  headline: String,
                  description: Option[String],
                  imageUrl: Option[String],
                  startTime: Option[Long],
                  updates: List[Update] = Nil,
                  updateFetchers: Map[String, String]= Map()) {

}

case class Update(`type`: String,
                  updateTime: Long,
                  updateHtml: String)

case class TimedUpdate(time: String, updates: Map[String, List[Update]], count: Int)

