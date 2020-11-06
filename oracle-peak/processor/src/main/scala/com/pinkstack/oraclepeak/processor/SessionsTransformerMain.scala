package com.pinkstack.oraclepeak.processor

import akka.actor.ActorSystem
import akka.kafka._
import akka.kafka.scaladsl.Consumer
import com.pinkstack.oraclepeak.core.Configuration
import com.typesafe.scalalogging.LazyLogging
import akka.NotUsed
import akka.stream.scaladsl._

import scala.util.{Failure, Success}
import org.apache.kafka.common.serialization._
import akka.stream.scaladsl.Sink
import com.pinkstack.oraclepeak.core.Model.{AccessPoint, Session}
import com.typesafe.config.Config
import io.circe.Decoder.Result
import org.apache.kafka.clients.consumer.ConsumerConfig

case class KafkaSessions()(implicit system: ActorSystem, config: Configuration.Config) {
  type Key = String
  type Value = String

  val (bootstrapServers: String, groupID: String, topics: Seq[String]) = (
    "one-cp-kafka-headless:9092",
    "sessions-processor-1",
    Seq("raw-sessions")
  )

  private lazy val consumerSettings: ConsumerSettings[String, String] = {

    val kafkaConsumer: Config = system.settings.config.getConfig("akka.kafka.consumer")
    ConsumerSettings(kafkaConsumer, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupID)
  }

  def source: Source[(Key, Value), Consumer.Control] = {
    Consumer.plainSource[Key, Value](consumerSettings, Subscriptions.topics(topics: _*))
      .map(record => (record.key(), record.value()))
  }
}

trait Location {
  def location: Option[String] = None
}

object TransformSessionFlow {

  import io.circe._, io.circe.parser._, io.circe.generic.auto._

  def apply() =
    Flow[(String, String)]
      .map { case (k: String, v: String) =>
        parse(v).map(_.as[Session].toOption)
      }
}

object SessionsTransformerMain extends App with LazyLogging {
  implicit val config: Configuration.Config = Configuration.load
  implicit val system: ActorSystem = ActorSystem("kafka-to-neo4j")

  import system.dispatcher

  KafkaSessions().source
    .via(TransformSessionFlow())
    .runWith(Sink.foreach(println))

}
