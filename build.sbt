import sbt._
import Keys._
import Settings._
import Dependencies._
import DockerSettings._
import sbt.Keys.resolvers
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._

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
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo-agent")))
)

lazy val agent = (project in file("oracle-peak/agent"))
  .withId("agent")
  .enablePlugins(BuildInfoPlugin)
  .settings(sharedSettings: _*)
  .settings(buildInfoPackage := "com.pinkstack.oraclepeak.agent")
  .settings(
    name := "agent",
  ).dependsOn(core)
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
