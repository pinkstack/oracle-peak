package com.pinkstack.oraclepeak.agent

import java.time.{LocalDateTime, ZoneOffset}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.pinkstack.oraclepeak.core.Configuration
import com.pinkstack.oraclepeak.core.Configuration.{ClientID, Location}
import com.pinkstack.oraclepeak.core.Model.{AccessPoint, CollectedSession, ISession, Session}

object SessionToMessage {

  import MMessage._
  import io.circe.generic.auto._
  import io.circe.syntax._

  def apply(implicit root: Path, config: Configuration.Config): Flow[Session, MMessage, NotUsed] = {
    // Should we emit everything to MQTT?
    val rich: Boolean = false

    Flow[Session]
      .map(s => CollectedSession(config.clientId, config.location, s.version, s.os, s.arch, s.wifi))
      .map { session: CollectedSession =>
        List[MMessage](
          MMessage("agent-version")(BuildInfo.version),
          MMessage("last-update")(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString),
          MMessage("session")(content = session.asJson.noSpacesSortKeys),
          MMessage("wifi-aps-size")(session.wifi.aps.size.toString),
          MMessage("wifi-aps-client-size")(session.wifi.aps.map(ap => ap.clients.size).sum.toString)
        ) ++ {
          if (rich)
            session.wifi.aps.flatMap { ap: AccessPoint =>
              val apPath = "access-points" / ap.mac
              List[MMessage](
                MMessage(apPath / "hostname")(ap.hostname.getOrElse("unknown")),
                MMessage(apPath / "rssi")(ap.rssi.toString),
                MMessage(apPath / "clients-size")(ap.clients.size.toString),
                MMessage(apPath / "last-seen")(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString)
              ) ++
                ap.clients.flatMap { client =>
                  val cPath = apPath / "client" / client.mac
                  List[MMessage](
                    MMessage(cPath / "hostname")(client.hostname.getOrElse("unknown")),
                    MMessage(cPath / "rssi")(client.rssi.toString),
                    MMessage(cPath / "last-seen")(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString)
                  )
                }
            }
          else Seq.empty[MMessage]
        }
      }.map(Source(_))
      .flatMapConcat(identity)
  }
}
