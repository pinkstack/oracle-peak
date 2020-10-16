package com.pinkstack.oraclepeak

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri, headers}
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.Future

object BetterCapClient {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe._

  private[this] def uri(implicit config: Configuration.Config): Uri =
    Uri(config.bettercap.url.toURI.toString)

  private[this] def baseRequest(implicit config: Configuration.Config): HttpRequest =
    HttpRequest().withHeaders(List(authorization))

  private[this] def authorization(implicit config: Configuration.Config) =
    headers.Authorization(headers.BasicHttpCredentials(config.bettercap.user, config.bettercap.password))

  private[this] def requestParse(request: HttpRequest)
                                (implicit system: ActorSystem, config: Configuration.Config): Future[Json] = {
    import system.dispatcher

    for {
      response <- Http().singleRequest(request)
      json <- Unmarshal(response).to[Json] if response.status.isSuccess()
    } yield json
  }

  def getSession(implicit system: ActorSystem, config: Configuration.Config): Future[Option[Vector[Json]]] = {
    import system.dispatcher
    val json: Future[Json] = requestParse(baseRequest.withUri(uri = uri.withPath(uri.path / "api" / "session")))
    val transform: Json => Json = _.hcursor.downField("wifi").downField("aps").focus.get
    json.map(transform).map(_.asArray)
  }
}
