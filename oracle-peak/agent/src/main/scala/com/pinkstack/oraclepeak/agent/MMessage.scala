package com.pinkstack.oraclepeak.agent

import akka.stream.alpakka.mqtt.MqttMessage
import akka.util.ByteString

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