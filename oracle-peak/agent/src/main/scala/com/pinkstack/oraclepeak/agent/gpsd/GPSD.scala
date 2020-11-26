package com.pinkstack.oraclepeak.agent.gpsd

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json

object GPSD extends LazyLogging {

  import scala.concurrent.duration._

  def source(host: String, port: Int)(implicit system: ActorSystem): Source[Json, NotUsed] = {
    logger.info(s"GPSD collection started on ${host}:${port}")
    Source.fromGraph(PositionGraphStage(host, port))
      .filterNot(_.isNull)
      .named("gpsd-source")
      .throttle(1, 1.seconds, 3, ThrottleMode.Shaping)
  }
}
