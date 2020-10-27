package com.pinkstack.oraclepeak.agent

import akka.stream.alpakka.mqtt.MqttConnectionSettings
import com.pinkstack.oraclepeak.core.Configuration
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

object MqttSettings {
  def apply(): MqttConnectionSettings = {
    val mqtt: Configuration.Mqtt = Configuration.load.mqtt
    MqttConnectionSettings(mqtt.broker, mqtt.clientId, new MemoryPersistence)
  }
}
