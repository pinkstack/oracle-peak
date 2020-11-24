package com.pinkstack.oraclepeak.agent.gpsd

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import io.circe.Json


object Client {
  type Hostname = String
  type Port = Int

  def props(hostname: Hostname, port: Port)
           (f: Json => Unit): Props =
    Props(new Client(hostname, port)(f))

  sealed trait Command

  sealed trait Event

  final case object GetCurrentPosition extends Command

  final case class CurrentPosition(json: io.circe.Json) extends Event

}

class Client(hostname: Client.Hostname, port: Client.Port)(f: Json => Unit) extends Actor with ActorLogging {

  import Client._
  import Tcp._
  import io.circe._
  import context.system

  final val NL: ByteString = ByteString("\n")
  final val WATCH: ByteString = ByteString("""?WATCH={"enable":true,"json":true}""" + "\n")

  @throws(classOf[Exception])
  private[this] val parseInput: String => Json = { input: String =>
    parser.parse(input).toOption match {
      case Some(json) => json
      case _ => throw new Exception(s"Failed parsing JSON input $input")
    }
  }

  private[this] def parseData(input: String)(f: Json => Unit): Unit =
    if (!input.contains("SKY") && (input.contains("TPV") || input.contains("VERSION")))
      input.split("\\n").map(parseInput).map(f)

  IO(Tcp) ! Connect(new InetSocketAddress(hostname, port))

  var latestPosition: Json = Json.Null

  override def receive: Receive = {
    case GetCurrentPosition =>
      sender() ! CurrentPosition(latestPosition)

    case CommandFailed(_: Connect) =>
      log.error("Connect has failed.")
      context.stop(self)

    case Connected(remote, local) =>
      log.info(s"Successfully connected to GPSD via $remote from $local.")

      val connection = sender()
      connection ! Register(self)

      context.become {
        case GetCurrentPosition =>
          sender() ! CurrentPosition(latestPosition)

        case Received(data: ByteString) =>
          parseData(data.utf8String)(self ! _)

        case json: Json =>
          json.hcursor.get[String]("class").toOption match {
            case Some(klass) if klass == "VERSION" =>
              connection ! Write(WATCH)
            case Some(klass) if klass == "TPV" =>
              latestPosition = json
              f(latestPosition)
              connection ! Write(NL)
            case Some(klass) =>
              log.warning(s"Got unhandled protocol class ${klass}")
              connection ! Write(NL)
            case None =>
              log.error("Received unknown class of message.")
              connection ! Write(NL)
          }

        case _: ConnectionClosed =>
          context.stop(self)
      }

  }
}
