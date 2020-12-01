package com.pinkstack.oraclepeak.agent

import java.time.{LocalDateTime, ZoneOffset}

import akka.stream.alpakka.mqtt.MqttMessage
import akka.util.ByteString
import com.pinkstack.oraclepeak.core.Configuration.Config
import io.circe.Json

case class MMessage(topic: String, payload: ByteString) {
  def asMqttMessage: MqttMessage = MqttMessage(topic, payload)

  override def toString: String = s"$topic: ${payload.decodeString("UTF-8")}"
}

object MMessage {
  type Path = String

  implicit class PathImprovements(val path: Path) {
    def /(other: String): Path = path.appended('/').appendedAll(other)
  }

  def apply(path: Path)(content: String)(implicit root: Path): MMessage =
    MMessage.apply(topic = root / path, payload = ByteString(content))

  def richMeta(implicit config: Config): Json =
    Json.fromFields(Seq(
      ("agent_version", Json.fromString(BuildInfo.version)),
      ("location", Json.fromString(config.location)),
      ("client_id", Json.fromString(config.clientId)),
      ("collected_at", Json.fromString(LocalDateTime.now().atOffset(ZoneOffset.UTC).toString))
    ))
}