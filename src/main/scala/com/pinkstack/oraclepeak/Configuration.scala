package com.pinkstack.oraclepeak

import pureconfig._
import pureconfig.generic.auto._
import pureconfig.ConfigReader.Result

object Configuration {

  final case class Bettercap(url: java.net.URL, user: String, password: String)

  final case class Neo4j(url: java.net.URI, user: String, password: String)

  final case class Mqtt(broker: String, clientId: String)

  final case class Config(bettercap: Bettercap, neo4j: Neo4j, mqtt: Mqtt)

  final def load: Config = ConfigSource.default.at("oracle-peak").loadOrThrow[Config]
}