package com.pinkstack.oraclepeak.bettercap

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri, headers}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.pinkstack.oraclepeak.Configuration
import com.pinkstack.oraclepeak.Model.Session

import scala.concurrent.Future

object WebClient {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe._

  private[this] def uri(implicit config: Configuration.Config): Uri =
    Uri(config.bettercap.url.toURI.toString)

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

  private[this] def baseRequest(implicit config: Configuration.Config): HttpRequest =
    HttpRequest().withHeaders(List(authorization))

  /*
  private[this] def define[T, TR](request: HttpRequest)
                                 (transform: Json => TR)
                                 (implicit system: ActorSystem, config: Configuration.Config): Future[TR] = {
    import system.dispatcher
    requestParse(request).map(transform) // .map(_.asArray)
  }

   */


  private[this] def execute[T](request: HttpRequest)
                              (transform: Json => T)
                              (implicit system: ActorSystem, config: Configuration.Config): Future[T] = {
    import system.dispatcher
    requestParse(request).map(transform)
  }

  def session(implicit system: ActorSystem, config: Configuration.Config): Future[Json] =
    execute(baseRequest.withUri(uri = uri.withPath(uri.path / "api" / "session"))) { json =>
      json.hcursor.value
    }

  def events(implicit system: ActorSystem, config: Configuration.Config): Future[Option[Vector[Json]]] =
    execute(baseRequest.withUri(uri = uri.withPath(uri.path / "api" / "events").withQuery(Uri.Query(("n", "50"))))) { json =>
      json.hcursor.value.asArray
    }
}
