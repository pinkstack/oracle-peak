package com.pinkstack.oraclepeak

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.LazyLogging
import akka.stream.scaladsl._
import io.circe.Json

import scala.concurrent.Future
import scala.concurrent.duration._

object BetterCapCollectionFlow {

  import Model.Tick

  def apply()(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, Json, NotUsed] =
    Flow[Tick]
      .mapAsync(2)(_ => BetterCapClient.getSession)
      .collect {
        case Some(jsons) =>
          Source(jsons)
      }.flatMapConcat(identity)
}


object CollectApp extends App with LazyLogging {

  import Model._

  implicit val system: ActorSystem = ActorSystem("collect")
  implicit val config: Configuration.Config = Configuration.load

  Source.tick(0.seconds, 2.seconds, Tick)
    .via(BetterCapCollectionFlow())
    // .map(_.noSpaces)
    .runWith(Sink.foreach(println))
}
