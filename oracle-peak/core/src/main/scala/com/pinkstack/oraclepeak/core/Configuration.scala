package com.pinkstack.oraclepeak.core

import pureconfig._
import pureconfig.generic.auto._
import pureconfig.ConfigReader.Result

object Configuration {
  type ClientID = String
  type Location = String

  final case class Bettercap(url: java.net.URL, user: String, password: String)

  final case class Neo4j(url: java.net.URI, user: String, password: String)

  final case class Mqtt(broker: String, clientId: ClientID, root: String, emit: Boolean)

  final case class Config(clientId: ClientID,
                          location: Location,
                          bettercap: Bettercap,
                          neo4j: Neo4j,
                          mqtt: Mqtt)

  final def load: Config = ConfigSource.default.at("oracle-peak").loadOrThrow[Config]
}