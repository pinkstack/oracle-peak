package com.pinkstack.oraclepeak.processor

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import com.pinkstack.oraclepeak.core.Model._
import com.pinkstack.oraclepeak.core.Configuration
// import com.pinkstack.oraclepeak.agent.bettercap.Flows
import com.typesafe.scalalogging.LazyLogging
import org.neo4j.driver._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.jdk.FutureConverters._
import scala.util.{Failure, Success}


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

  def deviceToNode(device: Device, id: String, kind: String*): Node = Node(id, device, kind: _*)

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

  implicit val system: ActorSystem = ActorSystem("collect")
  implicit val config: Configuration.Config = Configuration.load

  scala.sys.addShutdownHook {
    logger.info("Terminating,...")
    system.terminate()
    Await.result(system.whenTerminated, 30.seconds)
  }
}
