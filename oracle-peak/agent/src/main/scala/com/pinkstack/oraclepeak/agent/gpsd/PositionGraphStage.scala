package com.pinkstack.oraclepeak.agent.gpsd

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.pinkstack.oraclepeak.agent.gpsd
import io.circe.Json

case class PositionGraphStage(host: String, port: Int)
                             (implicit system: ActorSystem) extends GraphStage[SourceShape[Json]] {
  val out: Outlet[Json] = Outlet("GPSDPosition.out")
  override val shape: SourceShape[Json] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var currentPosition: Json = Json.Null
      system.actorOf(gpsd.Client.props(host, port)(currentPosition = _), name = "client")

      setHandler(out, new OutHandler {
        override def onPull(): Unit = push(out, currentPosition)
      })
    }
}
