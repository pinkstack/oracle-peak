package com.pinkstack.oraclepeak.agent

import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.ActorSystem
import akka.stream.RestartSettings
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import com.pinkstack.oraclepeak.Model._
import com.pinkstack.oraclepeak.{Configuration, bettercap}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration._

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

object Agent extends App with LazyLogging {
  implicit val config: Configuration.Config = Configuration.load
  implicit val system: ActorSystem = ActorSystem("agent")
  implicit val root: MMessage.Path = config.mqtt.root

  println(
    s"""\n🏔 🏔 Oracle Peak Agent 🏔 🏔\n
       |Java Version: ${System.getProperty("java.version")}
       |Java VM Version: ${System.getProperty("java.vm.version")}
       |Java VM Name: ${System.getProperty("java.vm.name")}
       |Java Runtime Version: ${System.getProperty("java.runtime.version")}
       |Java Runtime Name: ${System.getProperty("java.runtime.name")}
       |OS ARCH: ${System.getProperty("os.arch")}
       |---
       |Name: ${BuildInfo.name}
       |SBT Version: ${BuildInfo.sbtVersion}
       |Scala Version: ${BuildInfo.scalaVersion}
       |Version: ${BuildInfo.version}
       |---
       |Bettercap URL: ${config.bettercap.url}
       |---
       |MQTT root: $root
       |MQTT broker: ${config.mqtt.broker}
       |MQTT emit out: ${config.mqtt.emit}
       |""".stripMargin)

  lazy val mqttSink: Sink[MqttMessage, Future[Done]] = {
    logger.info(s"MQTT Sink with broker ${config.mqtt.broker} with root $root")
    // AtMostOnce = 0 , AtLeastOnce = 1
    MqttSink(MqttSettings().withConnectionTimeout(5.seconds), MqttQoS.AtMostOnce)
  }

  def EndSink: Sink[MqttMessage, Future[Done]] =
    Option.when(config.mqtt.emit)(mqttSink).getOrElse(Sink.foreach[MqttMessage](println))

  val f = Source.tick(2.seconds, 2.second, Tick)
    .via(RestartFlow.withBackoff(RestartSettings(10.seconds, 2.minutes, 0.4))(() =>
      bettercap.Flows.sessions())
    )
    .via(SessionToMessage.apply)
    .map(_.asMqttMessage)
    .runWith(RestartSink.withBackoff(RestartSettings(6.seconds, 20.seconds, 0.2)) {
      () => EndSink
    })
}
