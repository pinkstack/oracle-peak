import sbt._
import Keys._
import Settings._
import Dependencies._
import DockerSettings._

lazy val core = (project in file("oracle-peak/core"))
  .withId("core")
  .settings(sharedSettings: _*)
  .settings(
    name := "core",
    libraryDependencies ++=
      Dependencies.akka ++
        Dependencies.fp ++
        Dependencies.circe ++
        Dependencies.configurationLibs ++
        Dependencies.logging ++
        Dependencies.testing,
    buildInfoPackage := "com.pinkstack.oraclepeak",
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-core")))
  )
  .enablePlugins(BuildInfoPlugin)

lazy val agent = (project in file("oracle-peak/agent"))
  .enablePlugins(BuildInfoPlugin)
  .withId("agent")
  .settings(sharedSettings: _*)
  .settings(
    name := "agent",
    libraryDependencies ++= Dependencies.Alpakka.mqtt,
    buildInfoPackage := "com.pinkstack.oraclepeak.agent",
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-agent")))
  )
  .dependsOn(core)
  .aggregate(core)

lazy val processor = (project in file("oracle-peak/processor"))
  .withId("processor")
  .settings(sharedSettings: _*)
  .settings(
    name := "processor",
    libraryDependencies ++= {
      Dependencies.Alpakka.kafka ++ Dependencies.neo4j
    },
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-processor")))
  )
  .dependsOn(core)
  .aggregate(core)

lazy val agentDefaultArch = agent
  .withId("agentDefaultArch")
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(defaultArchSettings: _*)
  .settings(target := {
    (ThisBuild / baseDirectory).value / "target" / "agentDefaultArch"
  })

lazy val agentArmV7 = agent
  .withId("agentArmV7")
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(armV7DockerSettings: _*)
  .settings(target := {
    (ThisBuild / baseDirectory).value / "target" / "agentArmV7"
  })

publishTo in ThisBuild := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-root")))
publishArtifact := false
