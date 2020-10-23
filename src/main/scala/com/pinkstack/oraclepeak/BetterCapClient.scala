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

  private[this] def define(request: HttpRequest)
                          (transform: Json => Json)
                          (implicit system: ActorSystem, config: Configuration.Config): Future[Option[Vector[Json]]] = {
    import system.dispatcher
    requestParse(request).map(transform).map(_.asArray)
  }

  def session(implicit system: ActorSystem, config: Configuration.Config): Future[Option[Vector[Json]]] =
    define(baseRequest.withUri(uri = uri.withPath(uri.path / "api" / "session"))) { json =>
      json.hcursor.downField("wifi").downField("aps").focus.get
    }

  def events(implicit system: ActorSystem, config: Configuration.Config): Future[Option[Vector[Json]]] =
    define(baseRequest.withUri(uri = uri.withPath(uri.path / "api" / "events").withQuery(Uri.Query(("n", "100")))))(_.hcursor.value)
}
