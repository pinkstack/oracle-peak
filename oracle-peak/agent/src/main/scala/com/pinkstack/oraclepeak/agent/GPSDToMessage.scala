package com.pinkstack.oraclepeak.agent

import java.time.{LocalDateTime, ZoneOffset}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.pinkstack.oraclepeak.core.Configuration
import io.circe.Json

object GPSDToMessage {

  import MMessage._
  import io.circe.generic.auto._
  import io.circe.syntax._

  def apply(implicit root: Path, config: Configuration.Config): Flow[Json, MMessage, NotUsed] =
    Flow[Json]
      .map { json =>
        List[MMessage](
          MMessage("tpv")(json.noSpacesSortKeys),
          MMessage("location") {
            (for {
              lat <- json.hcursor.get[Double]("lat").toOption
              lon <- json.hcursor.get[Double]("lon").toOption
            } yield Json.fromFields(Seq(
              ("client_id", Json.fromString(config.clientId)),
              ("location", Json.fromString(config.location)),
              ("collected_at", Json.fromString(LocalDateTime.now.atOffset(ZoneOffset.UTC).toString)),
              ("location", Json.fromFields(Seq(
                ("lat", Json.fromDoubleOrNull(lat)),
                ("lon", Json.fromDoubleOrNull(lon)),
              )))))).getOrElse(Json.Null).noSpacesSortKeys
          }
        )
      }.map(Source(_))
      .flatMapConcat(identity)
}
