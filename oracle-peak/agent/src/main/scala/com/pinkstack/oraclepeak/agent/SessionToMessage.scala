package com.pinkstack.oraclepeak.agent

import java.time.{LocalDateTime, ZoneOffset}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.pinkstack.oraclepeak.Model.{AccessPoint, Session}

object SessionToMessage {

  import MMessage._
  import io.circe.generic.auto._
  import io.circe.syntax._

  def apply(implicit root: Path): Flow[Session, MMessage, NotUsed] = {
    // Should we emit everything to MQTT?
    val rich: Boolean = false

    Flow[Session].map { session: Session =>
      List[MMessage](
        MMessage("agent-version")(BuildInfo.version),
        MMessage("last-update")(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString),
        MMessage("session")(content = session.asJson.toString),
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
