package com.pinkstack.oraclepeak.agent

import java.time.{LocalDateTime, ZoneOffset}

import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.scaladsl.MqttSink
import akka.stream.alpakka.mqtt.{MqttMessage, MqttQoS}
import akka.stream.scaladsl._
import akka.stream.{Attributes, ClosedShape, RestartSettings}
import akka.{Done, NotUsed}
import com.pinkstack.oraclepeak.agent.gpsd.GPSD
import io.circe.Json
import com.pinkstack.oraclepeak.core.Configuration
import com.pinkstack.oraclepeak.core.Model._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration._

object Agent extends App with LazyLogging {

  import MMessage._

  implicit val system: ActorSystem = ActorSystem("agent")
  implicit val config: Configuration.Config = Configuration.load
  implicit val root: MMessage.Path = config.mqtt.root / config.location / config.clientId

  val loggingAttributes = Attributes.logLevels(
    // onElement = Attributes.LogLevels.Info,
    onFinish = Attributes.LogLevels.Debug,
    onFailure = Attributes.LogLevels.Debug)

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

  lazy val mqttSink: Sink[MqttMessage, Future[Done]] = {
    logger.info(s"MQTT Sink with broker ${config.mqtt.broker} with root $root")
    MqttSink(MqttSettings().withConnectionTimeout(5.seconds), MqttQoS.AtMostOnce)
  }

  RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val endSink: Sink[MqttMessage, Future[Done]] =
      Option.when(config.mqtt.emit)(mqttSink)
        .getOrElse(Sink.foreach[MqttMessage](m => println(m.payload.utf8String)))

    val restartableEnd = Flow[MMessage]
      .map(_.asMqttMessage)
      .to(RestartSink.withBackoff(RestartSettings(6.seconds, 20.seconds, 0.2))(() => endSink))

    val gpsdSource: Source[MMessage, NotUsed] = {
      val (host: String, port: Int) = (config.gpsd.url.getHost, config.gpsd.url.getPort)
      val transform = Flow[Json].map { json =>
        Source(List[MMessage](
          MMessage("tpv")(json.noSpacesSortKeys),
          MMessage("location") {
            (for {
              lat <- json.hcursor.get[Double]("lat").toOption
              lon <- json.hcursor.get[Double]("lon").toOption
            } yield Json.fromFields(Seq(
              ("client_id", Json.fromString(config.clientId)),
              ("location", Json.fromString(config.location)),
              ("collected_at", Json.fromString(LocalDateTime.now.atOffset(ZoneOffset.UTC).toString)),
              ("location", Json.fromFields(Seq(
                ("lat", Json.fromDoubleOrNull(lat)),
                ("lon", Json.fromDoubleOrNull(lon)),
              )))))).getOrElse(Json.Null).noSpacesSortKeys
          }
        ))
      }.flatMapConcat(identity)

      Option.when(config.gpsd.enabled)(GPSD.source(host, port).via(transform)).getOrElse(Source.never)
    }

    val sessions: Source[MMessage, _] = Option.when(config.bettercap.enabled) {
      Source.tick(2.seconds, 2.second, Tick)
        .via(RestartFlow.withBackoff(RestartSettings(10.seconds, 2.minutes, 0.4))(() =>
          bettercap.Flows.sessions())
        )
        .via(SessionToMessage.apply)
    }.getOrElse(Source.never[MMessage])

    val merge = builder.add(Merge[MMessage](2))
    val out = builder.add(restartableEnd).in

    sessions ~> merge.in(0)
    gpsdSource ~> merge.in(1)
    merge ~> out

    ClosedShape
  }).run()
}
