package com.pinkstack.oraclepeak

import java.net.{URI, URL}

import pureconfig._
import pureconfig.generic.auto._
import akka.http.scaladsl.model.Uri
import pureconfig.ConfigReader.Result

object Configuration {

  final case class Bettercap(url: URL, user: String, password: String)

  final case class Neo4j(url: URI, user: String, password: String)

  final case class Config(bettercap: Bettercap, neo4j: Neo4j)

  final def load: Config = ConfigSource.default.at("oracle-peak").loadOrThrow[Config]
}