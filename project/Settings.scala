import sbt._
import sbt.Keys._
import sbt.nio.Keys._

object Settings {
  lazy val sharedSettings = Seq(
    // version := "0.0.1",

    scalaVersion := "2.13.3",
    organizationName := "Pinkstack",
    organization := "com.pinkstack.oraclepeak",
    homepage := Some(url("https://github.com/pinkstack/oracle-peak")),

    Compile / packageDoc / mappings := Seq(),
    Global / onChangedBuildSource := ReloadOnSourceChanges,

    Test / fork := true,
    Test / javaOptions ++= Seq("-Xmx2g"),

    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-explaintypes",
      "-unchecked",
      "-Xlint:-unused,_",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:postfixOps",
      "-Yrangepos"
      // "-target:jvm-1.11"
    ),

    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.bintrayRepo("ovotech", "maven"),
      "Confluent Maven Repository" at "https://packages.confluent.io/maven/",
      "jitpack" at "https://jitpack.io"
    ),

    publish := {},
    publishLocal := {},
    publishArtifact := false,
    publishMavenStyle := false,
    publishArtifact := false,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
  )
}
