package com.pinkstack.oraclepeak.agent.bettercap

import java.security.MessageDigest
import java.time.{LocalDateTime, ZoneOffset}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import com.pinkstack.oraclepeak.agent.{BuildInfo, MMessage}
import com.pinkstack.oraclepeak.core.Configuration
import com.pinkstack.oraclepeak.core.Configuration.Config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

object Flows extends LazyLogging {

  import com.pinkstack.oraclepeak.core.Model._
  import io.circe._
  import io.circe.generic.auto._

  def rawSessions()(implicit system: ActorSystem, configuration: Config): Flow[Tick, Json, NotUsed] = {
    logger.info(s"Bettercap collection started on ${configuration.bettercap.url}")
    Flow[Tick].mapAsyncUnordered(parallelism = 2)(_ => WebClient.session)
      .named("rawSession")
  }

  def sessions()(implicit system: ActorSystem, configuration: Config): Flow[Tick, Session, NotUsed] =
    rawSessions
      .map(_.as[Session].toOption)
      .collect {
        case Some(value: Session) => value
        case None => throw new Exception("Failed parsing session")
      }.named("session")

  def accessPoints()(implicit system: ActorSystem, configuration: Configuration.Config): Flow[Tick, AccessPoint, NotUsed] =
    sessions
      .map(_.wifi.aps)
      .map(aps => Source(aps))
      .flatMapConcat(identity)
      .named("accessPoints")


  type EventKey = String

  def rawEvents()(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, (EventKey, Json), NotUsed] = {
    import system.dispatcher

    val transformEvent: Json => (EventKey, Json) = { json =>
      val addKey: Json => (EventKey, Json) = { j =>
        (for {
          tag <- j.hcursor.get[String]("tag").toOption
          time <- j.hcursor.get[String]("time").toOption
          rawKey = List(tag, time, config.clientId, config.location).mkString("-")
          key = MessageDigest.getInstance("SHA-256")
            .digest(rawKey.getBytes("UTF-8"))
            .map("%02x".format(_)).mkString
        } yield (key, Json.fromFields(Seq[(EventKey, Json)](("key", Json.fromString(key))))))
          .getOrElse {
            ("", Json.fromValues(Seq.empty[Json]))
          }
      }

      (addKey andThen { case (k, v) => (k, json.deepMerge(v)) }) (json)
    }

    Flow[Tick]
      .mapAsyncUnordered(parallelism = 2)(_ =>
        WebClient.events.map(_.getOrElse(List.empty[Json]))
      )
      .map(Source(_))
      .flatMapConcat(identity)
      .map(transformEvent)
  }

  def events()(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, Json, NotUsed] = {
    rawEvents
      .map { case (k, v) => (k, MMessage.richMeta.deepMerge(v)) }
      .statefulMapConcat { () =>
        var seen = mutable.Queue.empty[EventKey]
        val maxSize = 50

        {
          case (key: EventKey, _) if seen.contains(key) =>
            Nil
          case (key: EventKey, json: Json) =>
            seen += key
            if (seen.size >= maxSize) seen.drop(0)
            json :: Nil
        }
      }
  }
}
