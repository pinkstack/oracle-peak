package com.pinkstack.oraclepeak.agent.gpsd

import java.time.{LocalDateTime, ZoneOffset}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.pinkstack.oraclepeak.agent.{BuildInfo, MMessage}
import com.pinkstack.oraclepeak.core.Configuration
import io.circe.Json

object ToMessage {

  import MMessage._

  private[this] def transformLocation(json: Json)(implicit config: Configuration.Config): Json = {
    for {
      lat <- json.hcursor.get[Double]("lat").toOption
      lon <- json.hcursor.get[Double]("lon").toOption
    } yield Json.fromFields(Seq(
      ("agent_version", Json.fromString(BuildInfo.version)),
      ("location", Json.fromString(config.location)),
      ("client_id", Json.fromString(config.clientId)),
      ("collected_at", Json.fromString(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString)),
      ("location", Json.fromFields(Seq(
        ("lat", Json.fromDoubleOrNull(lat)),
        ("lon", Json.fromDoubleOrNull(lon)),
      )))))
  }.getOrElse(Json.Null)


  def apply(implicit root: Path, config: Configuration.Config): Flow[Json, MMessage, NotUsed] =
    Flow[Json]
      .map { json =>
        List[MMessage](
          MMessage("tpv")(json.noSpacesSortKeys),
          MMessage("location")(transformLocation(json).noSpacesSortKeys)
        )
      }.map(Source(_))
      .flatMapConcat(identity)
}
