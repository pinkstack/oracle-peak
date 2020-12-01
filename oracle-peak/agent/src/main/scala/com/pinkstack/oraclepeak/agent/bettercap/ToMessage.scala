package com.pinkstack.oraclepeak.agent.bettercap

import java.time.{LocalDateTime, ZoneOffset}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.pinkstack.oraclepeak.agent.{BuildInfo, MMessage}
import com.pinkstack.oraclepeak.core.Configuration
import io.circe.optics.JsonPath.root

object ToMessage {

  import MMessage._
  import io.circe._

  private[this] def transformSession(json: Json)(implicit config: Configuration.Config) = {
    val transformAccessPoint: Json => Json = { json =>
      val removeMeta = root.each.obj.modify(o => o.remove("meta"))
      val removeClientMeta = root.each.clients.each.obj.modify(o => o.remove("meta"))

      (removeMeta andThen removeClientMeta) (json)
    }

    MMessage.richMeta.deepMerge(
      Json.fromFields(Seq(
        ("aps", json.hcursor.downField("wifi").downField("aps")
          .focus.map(transformAccessPoint).getOrElse(Json.fromValues(Array.empty[Json])))
      )))
  }

  def fromJson(implicit root: Path, config: Configuration.Config): Flow[Json, MMessage, NotUsed] =
    Flow[Json]
      .map { json =>
        List[MMessage](
          MMessage("agent-version")(BuildInfo.version),
          MMessage("last-update")(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString),
          MMessage("session")(transformSession(json).noSpacesSortKeys)
        )
      }
      .map(Source(_))
      .flatMapConcat(identity)
}
