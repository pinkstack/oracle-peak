package com.pinkstack.oraclepeak.agent

import java.time.{LocalDateTime, ZoneOffset}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.scaladsl._
import akka.stream.alpakka.mqtt._
import akka.stream.scaladsl._
import com.pinkstack.oraclepeak.Configuration
import com.pinkstack.oraclepeak.Model._
import com.pinkstack.oraclepeak.bettercap
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Agent extends App with LazyLogging {
  implicit val config: Configuration.Config = Configuration.load
  implicit val system: ActorSystem = ActorSystem("agent")

  import MMessage._
  import io.circe.generic.auto._
  import io.circe.syntax._
  import system.dispatcher

  implicit val root: MMessage.Path = config.mqtt.root

  println {
    s""" 🏔 Oracle Peak Agent 🏔
       | Java Version: ${System.getProperty("java.version")}
       | Java VM Version: ${System.getProperty("java.vm.version")}
       | Java VM Name: ${System.getProperty("java.vm.name")}
       | Java Runtime Version: ${System.getProperty("java.runtime.version")}
       | Java Runtime Name: ${System.getProperty("java.runtime.name")}
       | OS ARCH: ${System.getProperty("os.arch")}
       | ---
       | Name: ${BuildInfo.name}
       | SBT Version: ${BuildInfo.sbtVersion}
       | Scala Version: ${BuildInfo.scalaVersion}
       | Version: ${BuildInfo.version}
       |""".stripMargin
  }

  lazy val mqttSink: Sink[MqttMessage, Future[Done]] = {
    logger.info(s"Booting MQTT Sink with broker: ${config.mqtt.broker} with root ${root}")
    MqttSink(MqttSettings().withCleanSession(true), MqttQoS.AtLeastOnce)
  }

  val f = Source.tick(0.seconds, 100.millisecond, Tick)
    .via(bettercap.Flows.sessions())
    .map { session: Session =>
      List[MMessage](
        MMessage("agent-version")(BuildInfo.version),
        MMessage("session")(content = session.asJson.toString),
        MMessage("wifi-aps-size")(session.wifi.aps.size.toString),
        MMessage("wifi-aps-client-size")(session.wifi.aps.map(ap => ap.clients.size).sum.toString)
      ) ++
        session.wifi.aps.flatMap { ap: AccessPoint =>
          val apPath = "access-points" / ap.mac
          List[MMessage](
            // MMessage(apPath / "hostname")(ap.hostname.getOrElse("unknown")),
            MMessage(apPath / "rssi")(ap.rssi.toString),
            MMessage(apPath / "clients-size")(ap.clients.size.toString),
            MMessage(apPath / "last-seen")(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString)
          ) ++
            ap.clients.flatMap { client =>
              val cPath = apPath / "client" / client.mac
              List[MMessage](
                // MMessage(cPath / "hostname")(client.hostname.getOrElse("unknown")),
                MMessage(cPath / "rssi")(client.rssi.toString),
                MMessage(cPath / "last-seen")(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString)
              )
            }
        }
    }
    .map { messages => Source(messages) }
    .flatMapConcat(identity)
    // .runWith(Sink.foreach(println))
    .map(_.asMqttMessage).runWith(mqttSink)

  f.onComplete {
    case Success(value) => value
    case Failure(exception) =>
      System.err.println(exception)
      system.terminate()
  }
}
