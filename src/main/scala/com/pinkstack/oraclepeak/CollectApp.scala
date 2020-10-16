package com.pinkstack.oraclepeak

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Attributes
import com.typesafe.scalalogging.LazyLogging
import akka.stream.scaladsl._
import com.pinkstack.oraclepeak.Model.{AccessPoint, Client, Device}
import io.circe.Json
import org.neo4j.driver._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
// import scala.collection.JavaConverters._
import scala.jdk.FutureConverters._
import scala.jdk.CollectionConverters._

object BetterCapCollectionFlow {

  import io.circe._, io.circe.parser._, io.circe.generic.auto._
  import Model._

  def apply()(implicit system: ActorSystem, config: Configuration.Config): Flow[Tick, AccessPoint, NotUsed] =
    Flow[Tick]
      .mapAsyncUnordered(2)(_ => BetterCapClient.getSession)
      .collect {
        case Some(jsons) =>
          Source(jsons)
      }
      .flatMapConcat(identity)
      .map(_.as[AccessPoint])
      .collect {
        case Right(accessPoint) => accessPoint
        case Left(exception) => throw exception
      }
}

object JsonToNeo4JFlow extends LazyLogging {

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

  def deviceToNode(device: Model.Device, id: String, kind: String*): Node =
    Node(id, device, kind: _*)

  def nodeToString(node: Node): String = {
    s"""MERGE (${node.symbol} {${fieldsFor(node.device)}})""" // -- RETURN ${node.id}
    // TODO: Add last seen
    // TODO: Add first detected
  }

  def relationshipToString(relationship: Relationship): String = {
    val (a_id, b_id) = (relationship.a.id, relationship.b.id)
    s"""MATCH (${relationship.a.symbol}), (${relationship.b.symbol})
       |  WHERE $a_id.mac = "${relationship.a.device.mac}" AND $b_id.mac = "${relationship.b.device.mac}"
       |  MERGE ($a_id)<-[r:CONNECTED]-($b_id)""".stripMargin
  }

  def apply()(implicit system: ActorSystem, driver: Driver) = {
    import system.dispatcher

    lazy val session = {
      logger.info("Booting Neo4j Session.")
      driver.asyncSession()
    }

    Flow[AccessPoint].map { ap =>
      val apNode = deviceToNode(ap, "d", "AccessPoint", "Device")
      val nodes = ap.clients.zipWithIndex.map { case (c, i) => deviceToNode(c, s"c_$i", "Client", "Device") }
      val relationships: List[Relationship] = nodes.map { node => Relationship(apNode, node) }

      Source {
        (List(apNode) ++ nodes ++ relationships).map {
          case n: Node => nodeToString(n)
          case r: Relationship => relationshipToString(r)
        }
      }
    }.flatMapConcat(identity)
      .log(name = "neo4jFlow")
      .addAttributes(
        Attributes.logLevels(
          onElement = Attributes.LogLevels.Debug,
          onFinish = Attributes.LogLevels.Info,
          onFailure = Attributes.LogLevels.Error))
      .map { query =>
        session.runAsync(query).asScala.onComplete {
          case Success(value) => value
          case Failure(exception) => throw exception
        }

        query
      }
  }
}

object CollectApp extends App with LazyLogging {

  import Model._

  implicit val system: ActorSystem = ActorSystem("collect")
  implicit val config: Configuration.Config = Configuration.load
  implicit val driver: Driver = GraphDatabase.driver(config.neo4j.url,
    AuthTokens.basic(config.neo4j.user, config.neo4j.password))

  system.registerOnTermination { () => driver.close() }
  scala.sys.addShutdownHook {
    logger.info("Terminating,...")
    system.terminate()
    Await.result(system.whenTerminated, 30.seconds)
  }

  Source.tick(0.seconds, 2.seconds, Tick)
    .via(BetterCapCollectionFlow())
    .via(JsonToNeo4JFlow())
    // .map(_.noSpaces)
    .runWith(Sink.foreach(println))

}
