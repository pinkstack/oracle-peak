package com.pinkstack.oraclepeak

import java.time.{LocalDate, LocalDateTime}

import akka._
import akka.actor.ActorSystem
import akka.stream.scaladsl._

import scala.concurrent.duration._

sealed trait Tick

case object Tick extends Tick

object Playground extends App {
  implicit val system = ActorSystem("playground")

  import system.dispatcher

  val f1 =
    Flow[LocalDateTime].map((localDateTime: LocalDateTime) => {
      val now = LocalDateTime.now()

      SourceWithContext
        .fromTuples(Source.single(s"Oto dude ${localDateTime}", localDateTime))
    }
    ).flatMapConcat(identity)
  // .asFlowWithContext // { case (s, l) => l } { case (s, l) => s }

  val S1 = SourceWithContext.fromTuples {
    val now = LocalDateTime.now()
    Source.tick(0.second, 2.second, () => LocalDateTime.now())
      .map(f => (f(), s"Hello this is context ${now}"))
  }

  val f = S1
    .map(l => l.toString().toLowerCase())
    .runWith(Sink.foreach(println))

}
