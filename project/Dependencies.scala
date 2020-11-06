import sbt._

object Dependencies {
  type Version = String
  private lazy val Akka: Version = "2.6.10"
  private lazy val AkkaHttp: Version = "10.2.1"
  private lazy val Circe: Version = "0.13.0"
  private lazy val Cats: Version = "2.2.0"

  lazy val akka: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor",
    "com.typesafe.akka" %% "akka-actor-typed",
    "com.typesafe.akka" %% "akka-stream"
  ).map(_ % Akka) ++ Seq(
    "com.typesafe.akka" %% "akka-http" % AkkaHttp,
    "de.heikoseeberger" %% "akka-http-circe" % "1.34.0"
  )

  object Alpakka {
    lazy val mqtt: Seq[ModuleID] = Seq(
      "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "2.0.2"
    )

    lazy val kafka: Seq[ModuleID] = Seq(
      "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.5"
    )
  }

  lazy val alpakka: Seq[ModuleID] = Alpakka.mqtt ++ Alpakka.kafka

  lazy val fp: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % Cats
  )

  lazy val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-generic-extras",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-optics",
    "io.circe" %% "circe-shapes"
  ).map(_ % Circe)

  lazy val configurationLibs: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % "1.4.0",
    "com.github.pureconfig" %% "pureconfig" % "0.14.0"
  )

  lazy val logging: Seq[ModuleID] = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.8"
  )

  lazy val testing: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.2.2"
  )

  lazy val neo4j: Seq[ModuleID] = Seq(
    "org.neo4j.driver" % "neo4j-java-driver" % "4.2.0-alpha01" // "4.1.1"
  )
}
