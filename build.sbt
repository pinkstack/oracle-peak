import Dependencies._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.0.1"
ThisBuild / organization := "com.pinkstack"
ThisBuild / organizationName := "pinkstack"
ThisBuild / resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)


lazy val root = (project in file("."))
  .settings(
    name := "Oracle Peak",
    libraryDependencies ++=
      Dependencies.akka ++
        Dependencies.fp ++
        Dependencies.circe ++
        Dependencies.configurationLibs ++
        Dependencies.logging ++
        Dependencies.neo4j ++
        Dependencies.testing
  )
