package com.gu.backchannel.model

import org.joda.time.DateTime
import com.novus.salat._
import com.mongodb.DBObject
import com.mongodb.casbah.commons.conversions.scala.{RegisterJodaTimeConversionHelpers, RegisterConversionHelpers}

object Preamble {
  implicit val dateOrdering: Ordering[DateTime] = new Ordering[DateTime] { def compare(d1: DateTime, d2: DateTime) = d1 compareTo d2 }
}

object SalatTypeConversions {
  implicit def caseClass2DBObject[A <: CaseClass](a: A)(implicit ctx: com.novus.salat.Context, m: scala.Predef.Manifest[A]): DBObject = {
    grater[A].asDBObject(a)
  }
}

trait CasbahConverstionHelpers {
  RegisterConversionHelpers()
  RegisterJodaTimeConversionHelpers()
}




