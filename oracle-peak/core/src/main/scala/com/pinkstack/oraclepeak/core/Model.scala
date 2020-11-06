package com.pinkstack.oraclepeak.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import io.circe._
import io.circe.optics.JsonPath.root

import scala.util.Try

object Model {

  sealed trait Tick

  final case object Tick extends Tick

  sealed trait Device extends Product with Serializable {
    // val ipv4: String
    // val ipv6: String
    val mac: String
    val hostname: Option[String]
    // val vendor: String
    // val frequency: Int
    // val channel: Int
    val rssi: Int
    // val sent: Int
    // val received: Int
    // val encryption: String
    // val cipher: String
    // val authentication: String
  }

  final case class Client( // ipv4: String,
                           // ipv6: String,
                           mac: String,
                           hostname: Option[String] = None,
                           // vendor: String,
                           // frequency: Int,
                           // channel: Int,
                           rssi: Int,
                           // sent: Int,
                           // received: Int,
                           // encryption: String,
                           // cipher: String,
                           // authentication: String,
                         ) extends Device

  final case class AccessPoint( // ipv4: String,
                                // ipv6: String,
                                mac: String,
                                hostname: Option[String],
                                // vendor: String,
                                // frequency: Int,
                                // channel: Int,
                                rssi: Int,
                                // sent: Int,
                                // received: Int,
                                // encryption: String,
                                // cipher: String,
                                // authentication: String,
                                clients: List[Client] = List.empty[Client]
                              ) extends Device

  trait ISession {
    def version: String

    def os: String

    def arch: String

    def wifi: Session.Wifi
  }

  case class Session(version: String, os: String, arch: String, wifi: Session.Wifi) extends ISession

  object Session {

    case class Wifi(aps: List[AccessPoint])

  }

  object Events {

    import io.circe.Decoder, io.circe.generic.auto._

    type EventTime = LocalDateTime

    sealed trait Event {
      val time: EventTime
    }

    case class WifiClientProbe(time: EventTime, data: WifiClientProbe.Data) extends Event {
    }

    object WifiClientProbe {

      case class Data(mac: String, vendor: String, alias: String, essid: String, rssi: Int)

    }

    case class WifiClientLost(time: EventTime, data: WifiClientLost.Data) extends Event

    object WifiClientLost {

      case class Data(`AP`: AccessPoint, `Client`: Client)

    }


    case class WifiClientNew(time: EventTime, data: WifiClientNew.Data) extends Event

    object WifiClientNew {

      case class Data(`AP`: AccessPoint, `Client`: Client)

    }

    case class WifiApNew(time: EventTime, data: AccessPoint) extends Event

    case class WifiApLost(time: EventTime, data: AccessPoint) extends Event

    private[this] def dateTimeFormatDecoder(format: DateTimeFormatter): Decoder[LocalDateTime] =
      Decoder[String].emapTry(s => Try(LocalDateTime.parse(s, format)))

    implicit val dateTimeEncoder: Decoder[LocalDateTime] = List(
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX"
    ).map(DateTimeFormatter.ofPattern)
      .map(dateTimeFormatDecoder)
      .reduceLeft((a, b) => a or b)

    implicit val decodeEventT: Decoder[Event] = {
      val tagToKind: Json => Json = root.string.modify(_.toLowerCase.split("\\.").toList
        .map(c => c.substring(0, 1).toUpperCase + c.substring(1)).mkString(""))

      Decoder.instance { c =>
        c.downField("tag").focus.map(tagToKind) match {
          case Some(tag) =>
            tag.asString match {
              case Some(s) if s == "WifiClientProbe" => c.as[WifiClientProbe]
              case Some(s) if s == "WifiClientLost" => c.as[WifiClientLost]
              case Some(s) if s == "WifiClientNew" => c.as[WifiClientNew]
              case Some(s) if s == "WifiApNew" => c.as[WifiApNew]
              case Some(s) if s == "WifiApLost" => c.as[WifiApLost]
              case None => throw new Exception(s"Unknown or unsupported event tag/type ${tag}")
            }
          case None => throw new Exception("No tag...")
        }
      }
    }
  }

}
