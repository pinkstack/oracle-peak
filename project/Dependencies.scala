import sbt._

object Dependencies {
  type Version = String
  private lazy val Akka: Version = "2.6.10"
  private lazy val AkkaHttp: Version = "10.2.1"
  private lazy val Circe: Version = "0.13.0"
  private lazy val Cats: Version = "2.0.0"

  lazy val akka: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % Akka,
    "com.typesafe.akka" %% "akka-actor-typed" % Akka,
    "com.typesafe.akka" %% "akka-stream" % Akka,
    "com.typesafe.akka" %% "akka-http" % AkkaHttp,

    // Circe + Akka HTTP
    "de.heikoseeberger" %% "akka-http-circe" % "1.34.0"
  )

  lazy val fp: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-core" % Cats
  )

  lazy val circe: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-generic-extras",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-optics"
  ).map(_ % Circe)

  lazy val configurationLibs: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % "1.4.0"
  )

  lazy val logging: Seq[ModuleID] = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.8"
  )


  lazy val testing: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.2.2"
  )
}
