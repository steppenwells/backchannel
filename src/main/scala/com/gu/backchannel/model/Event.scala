package com.gu.backchannel.model

import com.novus.salat.annotations._
import org.bson.types.ObjectId


case class Event(@Key("_id") id: String = new ObjectId().toString,
                  headline: String,
                  description: Option[String],
                  imageUrl: Option[String],
                  startTime: Option[Long],
                  updates: List[Update] = Nil) {

}

case class Update(`type`: String,
                  updateTime: Long,
                  updateHtml: String)

