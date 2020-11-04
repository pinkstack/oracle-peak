package com.pinkstack.oraclepeak.processor

import akka.actor.ActorSystem
import akka.kafka._
import akka.kafka.scaladsl.Consumer
import com.pinkstack.oraclepeak.core.Configuration
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success}
import org.apache.kafka.common.serialization._
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig

object Kafka2Neo4jMain extends App with LazyLogging {
  implicit val config: Configuration.Config = Configuration.load
  implicit val system: ActorSystem = ActorSystem("kafka-to-neo4j")

  import system.dispatcher

  val configTwo = system.settings.config.getConfig("akka.kafka.consumer")
  val bootstrapServers: String = "kafka-one-cp-kafka-headless:9092"

  val consumerSettings =
    ConsumerSettings(configTwo, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId("group1")
  // .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
  // .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val done = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("wifi-aps-client-sizes"))
    .map(msg => {
      (
        msg.key(),
        msg.value()
      )
    })
    .runWith(Sink.foreach(println))

  done.onComplete {
    case Success(value) => println(value)
    case Failure(exception) => System.err.println(exception.getMessage)
  }
}
