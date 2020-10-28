package com.pinkstack.oraclepeak.agent

import akka.Done
import akka.actor.ActorSystem
import akka.stream.RestartSettings
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl._
import akka.stream.scaladsl._
import com.pinkstack.oraclepeak.core.Model._
import com.pinkstack.oraclepeak.core.Configuration
import com.pinkstack.oraclepeak.core.Configuration._
import com.pinkstack.oraclepeak.core.bettercap

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration._

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
