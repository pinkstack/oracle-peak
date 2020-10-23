package com.pinkstack.oraclepeak

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats._
import cats.implicits._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream._
import com.typesafe.scalalogging.LazyLogging
import akka.stream.scaladsl._
import com.pinkstack.oraclepeak.Model.Events.Event
import com.pinkstack.oraclepeak.Model._
import io.circe.Json
import io.circe.optics.JsonPath.root
import org.neo4j.driver._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.jdk.FutureConverters._
import scala.jdk.CollectionConverters._

object BettercapCollectionFlow {

  import io.circe._, io.circe.parser._, io.circe.generic.auto._
  import Model._

  private[this] def define[T](f: Future[Option[Vector[Json]]], parallelism: Int = 2)
                             (transformation: Json => Either[Exception, T])
                             (implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, T, NotUsed] =
    Flow[Tick]
      .mapAsyncUnordered(parallelism)(_ => f)
      .collect {
        case Some(jsons) => Source(jsons)
        case None => throw new Exception("Problem fetching data from Bettercap server")
      }
      .flatMapConcat(identity)
      .map(transformation)
      .collect {
        case Right(value) => value
        case Left(exception) => throw exception
      }

  def accessPoints()(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, AccessPoint, NotUsed] =
    define(BetterCapClient.session, 2)(_.as[AccessPoint])

  def events()(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, Event, NotUsed] =
    define(BetterCapClient.events, 2)(_.as[Event])
}

object Neo4jSink extends LazyLogging {

  sealed trait Element

  final case class Node(id: String, device: Device, kind: String*) extends Element {
    def symbol: String = (List(id) ++ kind.toList).mkString(":")
  }

  final case class Relationship(a: Node, b: Node) extends Element

  val fieldsFor: Device => String = device =>
    device.productElementNames.zip(device.productIterator)
      .filter {
        case (_, _: List[Any]) => false
        case _ => true
      }.map {
      case (k, v: Int) => s"""$k: $v"""
      case (k, v: Any) => s"""$k: '$v'"""
    }.mkString(", ").strip()

  def deviceToNode(device: Model.Device, id: String, kind: String*): Node = Node(id, device, kind: _*)

  private[this] def neo4jSink()(implicit system: ActorSystem, config: Configuration.Config): Sink[Element, Future[Done]] = {
    import system.dispatcher
    implicit val driver: Driver = GraphDatabase.driver(config.neo4j.url,
      AuthTokens.basic(config.neo4j.user, config.neo4j.password))

    implicit val nodeToString: Node => String = node =>
      s"""MERGE (${node.symbol} {mac: '${node.device.mac}', hostname: '${node.device.hostname}'})
         |  ON CREATE SET ${node.id}.created = timestamp(), ${node.id}.rssi = ${node.device.rssi}
         |  ON MATCH SET ${node.id}.lastSeen = timestamp(), ${node.id}.rssi = ${node.device.rssi}
         |  RETURN ${node.id}""".stripMargin

    implicit val relationshipToString: Relationship => String = { relationship =>
      val (a_id, b_id) = (relationship.a.id, relationship.b.id)
      s"""MATCH (${relationship.a.symbol} {mac: "${relationship.a.device.mac}"}), (${relationship.b.symbol} {mac: "${relationship.b.device.mac}"})
         |  MERGE ($a_id)<-[r:CONNECTED]-($b_id)
         |    ON CREATE SET $a_id.created = timestamp(),
         |                  $b_id.created = timestamp(),
         |                  $b_id.rssi = ${relationship.b.device.rssi}
         |    ON MATCH SET $a_id.lastSeen = timestamp(),
         |                 $b_id.lastSeen = timestamp(),
         |                 $b_id.rssi = ${relationship.b.device.rssi}
         |                 """.stripMargin
    }

    implicit val elementToQueryString: Element => String = {
      case n: Node => nodeToString(n)
      case r: Relationship => relationshipToString(r)
    }

    lazy val session = {
      logger.info("Booting Neo4j Session.")
      driver.asyncSession()
    }

    Sink.foreach[Element](session.runAsync(_).asScala.onComplete {
      case Success(value) => value
      case Failure(ex) => throw ex
    })
  }

  def apply()(implicit system: ActorSystem, config: Configuration.Config): Sink[AccessPoint, NotUsed] =
    Flow[AccessPoint].map { ap =>
      val apNode = deviceToNode(ap, "d", "AccessPoint", "Device")
      val nodes = ap.clients.zipWithIndex.map { case (c, i) => deviceToNode(c, s"c_$i", "Client", "Device") }
      val relationships: List[Relationship] = nodes.map { node => Relationship(apNode, node) }
      Source(List(apNode) ++ nodes ++ relationships)
    }.flatMapConcat(identity)
      .to(neo4jSink())
}

object CollectApp extends App with LazyLogging {

  import Model._

  implicit val system: ActorSystem = ActorSystem("collect")

  import system.dispatcher

  implicit val config: Configuration.Config = Configuration.load

  scala.sys.addShutdownHook {
    logger.info("Terminating,...")
    system.terminate()
    Await.result(system.whenTerminated, 30.seconds)
  }

  /*

    Source.tick(0.seconds, 2.seconds, Tick)
      .via(BetterCapCollectionFlow.accessPoints())
      .runWith(Neo4jSink())
  */


  Source.tick(0.seconds, 2.seconds, Tick)
    .via(BettercapCollectionFlow.accessPoints())
    .runWith(Sink.foreach(println))

  val f = Source.tick(0.seconds, 2.seconds, Tick)
    .log("log")
    .addAttributes(Attributes.logLevels(
      onElement = Attributes.LogLevels.Debug,
      onFinish = Attributes.LogLevels.Debug,
      onFailure = Attributes.LogLevels.Error))
    .via(BettercapCollectionFlow.events())
    .runWith(Sink.foreach(println))

  f.onComplete {
    case Success(value) => println(value)
    case Failure(exception) => System.err.println(exception)
  }
}
