package com.pinkstack.oraclepeak

import java.time.{LocalDateTime, ZoneId, ZoneOffset}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS}
import akka.stream.scaladsl._
import akka.stream.alpakka.mqtt.scaladsl._
import akka.util.ByteString
import com.pinkstack.oraclepeak.Model._
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MqttSettings {
  def apply(): MqttConnectionSettings = {
    val mqtt: Configuration.Mqtt = Configuration.load.mqtt
    MqttConnectionSettings(mqtt.broker, mqtt.clientId, new MemoryPersistence)
  }
}

case class MMessage(topic: String, payload: ByteString) {
  def asMqttMessage: MqttMessage = MqttMessage(topic = topic, payload = payload)

  override def toString: String = s"$topic: ${payload.decodeString("UTF-8")}"
}

object MMessage {
  type Path = String

  implicit class PathImprovements(val path: Path) {
    def /(other: String): Path = path + "/" + other
  }

  def apply(path: Path)(content: String)(implicit root: Path): MMessage =
    MMessage.apply(topic = root / path, payload = ByteString(content))
}

object AgentApp extends App with LazyLogging {
  implicit val config: Configuration.Config = Configuration.load
  implicit val system: ActorSystem = ActorSystem("collect")

  import system.dispatcher
  import MMessage._
  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

  implicit val root: MMessage.Path = "oracle-peak-development" / "experiment-1"

  lazy val mqttSink: Sink[MqttMessage, Future[Done]] = {
    logger.info(s"Booting MQTT Sink,...")
    // AtMostOnce => 0 => Fast no ACK
    // AtLeastOnce => 1 => Basic ACK flow
    MqttSink(MqttSettings().withCleanSession(true), MqttQoS.AtLeastOnce)
  }

  val f = Source.tick(0.seconds, 100.millisecond, Tick)
    .via(bettercap.Flows.sessions())
    .map { session: Session =>
      List[MMessage](
        MMessage("session")(session.asJson.toString()),
        MMessage("wifi-aps-size")(session.wifi.aps.size.toString),
        MMessage("wifi-aps-client-size")(session.wifi.aps.map(ap => ap.clients.size).sum.toString)
      ) ++
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
    }
    .map { messages => Source(messages) }
    .flatMapConcat(identity)
    // .runWith(Sink.foreach(println))
    .map(_.asMqttMessage).runWith(mqttSink)

  f.onComplete {
    case Success(value) =>
    case Failure(exception) =>
      System.err.println(exception)
      system.terminate()
  }
}
