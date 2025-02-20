import sbt._
import sbt.Keys._
import sbt.nio.Keys._
import sbt.{Def, Resolver, _}

object Settings {
  lazy val sharedSettings = Seq(
    scalaVersion := "2.13.3",
    organizationName := "Pinkstack",
    organization := "com.pinkstack.oraclepeak",
    homepage := Some(url("https://github.com/pinkstack/oracle-peak")),

    organizationHomepage := Some(new URL("https://github.com/pinkstack")),
    description := "Service and components for smart Oracle Peak monitoring",
    apiURL := Some(new URL("https://github.com/pinkstack")),
    developers := List(
      Developer(id = "otobrglez", name = "Oto Brglez", email = "otobrglez@gmail.com", url = new URL("http://www.opalab.com"))
    ),
    licenses += ("MIT", new URL("https://opensource.org/licenses/MIT")),
    licenses += ("BSD-2-Clause-Patent", new URL("https://opensource.org/licenses/BSDplusPatent")),

    Compile / packageDoc / mappings := Seq(),
    Global / onChangedBuildSource := ReloadOnSourceChanges,

    Test / fork := true,
    Test / javaOptions ++= Seq("-Xmx2g"),

    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-feature",
      "-explaintypes",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:postfixOps",
      "-Yrangepos",
      "-target:11"
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
    publishMavenStyle := false,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
  )
}
