package com.pinkstack.oraclepeak.bettercap

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import com.pinkstack.oraclepeak.Model.Events.Event
import com.pinkstack.oraclepeak._

object Flows {

  import Model._
  import io.circe._
  import io.circe.generic.auto._

  def sessions()(implicit system: ActorSystem, configuration: Configuration.Config): Flow[Tick, Session, NotUsed] = {
    import system.dispatcher
    Flow[Tick].mapAsyncUnordered(parallelism = 2)(_ =>
      WebClient.session.map(_.as[Session].toOption)
    ).collect {
      case Some(value: Session) => value
      case None => throw new Exception("Failed parsing session")
    }.named("session")
  }

  def accessPoints()(implicit system: ActorSystem, configuration: Configuration.Config): Flow[Tick, AccessPoint, NotUsed] =
    sessions()
      .map(_.wifi.aps)
      .map(aps => Source(aps))
      .flatMapConcat(identity)
      .named("accessPoints")

  def events()(implicit system: ActorSystem, configuration: Configuration.Config): Flow[Tick, Event, NotUsed] = {
    import system.dispatcher

    Flow[Tick]
      .mapAsyncUnordered(parallelism = 2)(_ =>
        WebClient.events.map(ov => ov.map(p => p.map(_.as[Event].toOption)))
      )
      .collect {
        case Some(value) => Source(value)
        case None => throw new Exception("Failed fetching events.")
      }
      .flatMapConcat(identity)
      .collect {
        case Some(value) => value
        case None => throw new Exception("Failed parsing JSON event.")
      }
      .named("events")
  }
}
