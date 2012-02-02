package com.gu.backchannel.model

import com.novus.salat.dao.SalatDAO
import com.mongodb.ServerAddress
import com.mongodb.casbah.{MongoURI, MongoDB, MongoConnection, WriteConcern}
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.global._


object Mongo {

  object EventDao extends SalatDAO[Event, String](collection = MongoDataSource.eventCollection)

  EventDao.collection.writeConcern = WriteConcern.Safe


  def loadEvent(id: String) = EventDao.findOneByID(id)

  def loadAllEvents = EventDao.find(MongoDBObject()).sort(orderBy = MongoDBObject("_id" -> 1)).toList

  def insert(event: Event) = EventDao.insert(event)

  def update(event: Event) = EventDao.save(event)

}



object MongoDataSource {

  val mongoDbName = "flexible-content"
  val mongoDbUsername = "fc"
  val mongoDbPassword = "fc"
  val host = "localhost:27017,localhost:27018,localhost:27019"

  val connection = MongoConnection(host.split(",").toList.map(server => new ServerAddress(server)))
  val db = connection(mongoDbName)

  if (!db.authenticate(mongoDbUsername, mongoDbPassword))
    throw new Exception("Authentication failed")

  lazy val eventCollection = createCollection("events")

  private def createCollection(name: String) = {
    val collection = db(name)
    collection.slaveOk()
    collection
  }

}