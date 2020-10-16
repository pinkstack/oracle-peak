package com.pinkstack.oraclepeak

object Model {

  sealed trait Tick

  final case object Tick extends Tick

  sealed trait Device extends Product with Serializable {
    // val ipv4: String
    // val ipv6: String
    val mac: String
    val hostname: String
    // val vendor: String
    // val frequency: Int
    // val channel: Int
    // val rssi: Int
    // val sent: Int
    // val received: Int
    // val encryption: String
    // val cipher: String
    // val authentication: String
  }

  final case class Client(// ipv4: String,
                          // ipv6: String,
                          mac: String,
                          hostname: String,
                          // vendor: String,
                          // frequency: Int,
                          // channel: Int,
                          // rssi: Int,
                          // sent: Int,
                          // received: Int,
                          // encryption: String,
                          // cipher: String,
                          // authentication: String,
                         ) extends Device

  final case class AccessPoint(// ipv4: String,
                               // ipv6: String,
                               mac: String,
                               hostname: String,
                               // vendor: String,
                               // frequency: Int,
                               // channel: Int,
                               // rssi: Int,
                               // sent: Int,
                               // received: Int,
                               // encryption: String,
                               // cipher: String,
                               // authentication: String,
                               clients: List[Client] = List.empty[Client]
                              ) extends Device

}
