package com.pinkstack.oraclepeak.agent

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.MqttQoS
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.scaladsl._
import akka.stream.{ClosedShape, RestartSettings}
import com.pinkstack.oraclepeak.agent.gpsd.GPSD
import com.pinkstack.oraclepeak.core.Configuration
import com.pinkstack.oraclepeak.core.Model._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._

object Agent extends App with LazyLogging {

  import MMessage._

  implicit val system: ActorSystem = ActorSystem("agent")
  implicit val config: Configuration.Config = Configuration.load
  implicit val root: MMessage.Path = config.mqtt.root / config.location / config.clientId

  // val loggingAttributes = Attributes.logLevels(
  //   onElement = Attributes.LogLevels.Info,
  //   onFinish = Attributes.LogLevels.Debug,
  //   onFailure = Attributes.LogLevels.Debug)

  println(
    s"""🏔 🏔 Oracle Peak Agent 🏔 🏔
       |Java VM Version and Name: ${System.getProperty("java.vm.version")} / ${System.getProperty("java.vm.name")}
       |Java Runtime Version and Name: ${System.getProperty("java.runtime.version")} / ${System.getProperty("java.runtime.name")}
       |Name: ${BuildInfo.name}, SBT Version: ${BuildInfo.sbtVersion}, Scala Version: ${BuildInfo.scalaVersion}
       |Oracle Peak Version: ${BuildInfo.version} on OS Arch: ${System.getProperty("os.arch")}
       |Client ID: ${config.clientId} / Location: ${config.location}
       |Bettercap URL: ${config.bettercap.url}
       |MQTT Client ID: ${config.clientId}
       |MQTT Root: $root
       |MQTT Broker: ${config.mqtt.broker}
       |MQTT Emit out: ${config.mqtt.emit}
       |GPSD URL: ${config.gpsd.url}
       |GPSD Enabled: ${config.gpsd.enabled}
       |""".stripMargin)

  lazy val mqttSink: Sink[MMessage, _] = {
    logger.info(s"MQTT Sink with broker ${config.mqtt.broker} with root $root")
    Flow[MMessage]
      .map(_.asMqttMessage)
      .to(MqttSink(MqttSettings().withConnectionTimeout(5.seconds), MqttQoS.AtMostOnce))
  }

  val restartableEnd = Flow[MMessage]
    .to(RestartSink.withBackoff(RestartSettings(6.seconds, 20.seconds, 0.2))(() =>
      Option.when(config.mqtt.emit)(mqttSink)
        .getOrElse(Sink.foreach[MMessage](m => println(m)))))

  RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val gpsdSource: Source[MMessage, NotUsed] = {
      val (host: String, port: Int) = (config.gpsd.url.getHost, config.gpsd.url.getPort)
      Option.when(config.gpsd.enabled)(GPSD.source(host, port)
        .via(gpsd.ToMessage.apply)).getOrElse(Source.never)
    }

    val sessions: Source[MMessage, _] =
      Option.when(config.bettercap.enabled) {
        Source.tick(2.seconds, 2.second, Tick)
          .via(RestartFlow.withBackoff(RestartSettings(10.seconds, 2.minutes, 0.4))(() =>
            bettercap.Flows.rawSessions())
          )
          .via(bettercap.ToMessage.fromJson)
      }.getOrElse(Source.never[MMessage])

    val events = Option.when(config.bettercap.enabled && config.bettercap.eventsEnabled) {
      Source.tick(1.seconds, 1.seconds, Tick)
        .via(RestartFlow.withBackoff(RestartSettings(10.seconds, 2.minutes, 0.4))(() =>
          bettercap.Flows.events()
        ).map { event =>
          MMessage("events")(event.noSpacesSortKeys)
        })
    }.getOrElse(Source.never[MMessage])

    val merge = builder.add(Merge[MMessage](3))
    val out = builder.add(restartableEnd).in

    sessions ~> merge.in(0)
    events ~> merge.in(1)
    gpsdSource ~> merge.in(2)
    merge ~> out

    ClosedShape
  }).run()
}
