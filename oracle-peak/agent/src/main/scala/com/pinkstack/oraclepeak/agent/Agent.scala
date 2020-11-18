package com.pinkstack.oraclepeak.agent

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{Attributes, RestartSettings}
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.pinkstack.oraclepeak.core.Model._
import com.pinkstack.oraclepeak.core.Configuration
import com.pinkstack.oraclepeak.core.bettercap
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration._

object Agent extends App with LazyLogging {
  implicit val config: Configuration.Config = Configuration.load
  implicit val system: ActorSystem = ActorSystem("agent")

  implicit val root: MMessage.Path = {
    import MMessage._
    config.mqtt.root / config.location / config.clientId
  }

  println(
    s"""🏔 🏔 Oracle Peak Agent 🏔 🏔
       |Java VM Version and Name: ${System.getProperty("java.vm.version")} / ${System.getProperty("java.vm.name")}
       |Java Runtime Version and Name: ${System.getProperty("java.runtime.version")} / ${System.getProperty("java.runtime.name")}
       |---
       |Name: ${BuildInfo.name}, SBT Version: ${BuildInfo.sbtVersion}, Scala Version: ${BuildInfo.scalaVersion}
       |Oracle Peak Version: ${BuildInfo.version} on OS Arch: ${System.getProperty("os.arch")}
       |---
       |Client ID: ${config.clientId} / Location: ${config.location}
       |---
       |Bettercap URL: ${config.bettercap.url}
       |---
       |MQTT Client ID: ${config.clientId}
       |MQTT Root: $root
       |MQTT Broker: ${config.mqtt.broker}
       |MQTT Emit out: ${config.mqtt.emit}
       |---
       |GPSD URL: ${config.gpsd.url}
       |GPSD Enabled: ${config.gpsd.enabled}
       |---
       |""".stripMargin)

  lazy val mqttSink: Sink[MqttMessage, Future[Done]] = {
    logger.info(s"MQTT Sink with broker ${config.mqtt.broker} with root $root")
    MqttSink(MqttSettings().withConnectionTimeout(5.seconds), MqttQoS.AtMostOnce)
  }

  def EndSink: Sink[MqttMessage, Future[Done]] =
    Option.when(config.mqtt.emit)(mqttSink).getOrElse(Sink.foreach[MqttMessage](m => println(m.payload.utf8String)))

  val sessionsRun: NotUsed = Option.when(config.bettercap.enabled)(
    Source.tick(2.seconds, 2.second, Tick)
      .via(RestartFlow.withBackoff(RestartSettings(10.seconds, 2.minutes, 0.4))(() =>
        bettercap.Flows.sessions())
      )
      .via(SessionToMessage.apply)
      .map(_.asMqttMessage)
      .runWith(RestartSink.withBackoff(RestartSettings(6.seconds, 20.seconds, 0.2)) { () =>
        EndSink
      })
  ).getOrElse(NotUsed)

  println("--- gpsd --- ")

  import io.circe._
  import io.circe.generic.auto._


  val printer = Flow[Json]
    .filter(_.hcursor.get[String]("class").toOption.contains("TPV"))
    .map { json =>
      import MMessage._

      val Array(lat, lon) = Array[Double](
        json.hcursor.get[Double]("lat").getOrElse(-1.0),
        json.hcursor.get[Double]("lon").getOrElse(-1.0)
      )
      val position = Json.fromFields(Seq(
        ("lat", Json.fromDoubleOrNull(lat)),
        ("lon", Json.fromDoubleOrNull(lon))
      ))

      List[MMessage](
        MMessage("gpsd")(json.noSpacesSortKeys),
        MMessage("position")(position.noSpacesSortKeys),
      )
    }
    .map(Source(_))
    .flatMapConcat(identity)
    .map(_.asMqttMessage)
    .to(RestartSink.withBackoff(RestartSettings(6.seconds, 20.seconds, 0.2)) { () =>
      EndSink
    })

  val inFlow = Flow[ByteString]
    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 2000, allowTruncation = true))
    .map(_.utf8String)
    .log("incoming", ex => ex)
    .addAttributes(Attributes.logLevels(
      onElement = Attributes.LogLevels.Debug,
      onFinish = Attributes.LogLevels.Debug,
      onFailure = Attributes.LogLevels.Debug))
    .map(parser.parse)
    .collect {
      case Right(json) => json
      case Left(ex) => throw ex
    }
    .alsoTo(printer)
    .map { message =>
      message.hcursor.get[String]("class") match {
        case Right(string) if string == "VERSION" =>
          ByteString("""?WATCH={"enable":true,"json":true}""" + "\n")
        case _ =>
          ByteString("\n")
      }
    }

  val r = Tcp(system).outgoingConnection(config.gpsd.url.getHost, config.gpsd.url.getPort)
    .join(inFlow)

  r.run()
}
