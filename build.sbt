import sbt._
import Keys._
import Settings._
import Dependencies._
import DockerSettings._

lazy val core = (project in file("oracle-peak/core"))
  .withId("core")
  .settings(sharedSettings: _*)
  .settings(name := "core")
  .settings(libraryDependencies ++=
    Dependencies.akka ++
      Dependencies.alpakka ++
      Dependencies.fp ++
      Dependencies.circe ++
      Dependencies.configurationLibs ++
      Dependencies.logging ++
      Dependencies.testing)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "com.pinkstack.oraclepeak"
  ).settings(
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-core")))
)

lazy val agent = (project in file("oracle-peak/agent"))
  .withId("agent")
  .settings(name := "agent")
  .enablePlugins(BuildInfoPlugin)
  .settings(sharedSettings: _*)
  .settings(buildInfoPackage := "com.pinkstack.oraclepeak.agent")
  .dependsOn(core)
  .aggregate(core)
  .settings(
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-agent")))
  )

lazy val processor = (project in file("oracle-peak/processor"))
  .withId("processor")
  .settings(sharedSettings: _*)
  .settings(name := "processor")
  .dependsOn(core)
  .settings(libraryDependencies ++=
    Dependencies.neo4j
  ).settings(
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-processor")))
)

lazy val agentDefaultArch = agent
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(defaultArchSettings: _*)
  .settings(target := {
    (ThisBuild / baseDirectory).value / "target" / "agentDefaultArch"
  })

lazy val agentArmV7 = agent
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(armV7DockerSettings: _*)
  .settings(target := {
    (ThisBuild / baseDirectory).value / "target" / "agentArmV7"
  })

publishTo in ThisBuild := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-root")))
publishArtifact := false
