import Dependencies._
import Settings._

lazy val core = (project in file("oracle-peak/core"))
  .settings(sharedSettings: _*)
  .settings(name := "core")
  .settings(libraryDependencies ++=
    Dependencies.akka ++
      Dependencies.alpakka ++
      Dependencies.fp ++
      Dependencies.circe ++
      Dependencies.configurationLibs ++
      Dependencies.logging ++
      Dependencies.neo4j ++
      Dependencies.testing)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "com.pinkstack.oraclepeak"
  )

lazy val agent = (project in file("oracle-peak/agent"))
  .dependsOn(core)
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging, DockerPlugin)
  .settings(sharedSettings: _*)
  .enablePlugins()
  .settings(
    name := "agent",
    buildInfoPackage := "com.pinkstack.oraclepeak.agent",

    // Docker Image
    maintainer in Docker := "Oto Brglez - <otobrglez@gmail.com>",
    dockerUsername := Some("pinkstack"),
    packageName in Docker := "oracle-peak-agent",
    dockerUpdateLatest := false,
    // Read: https://hub.docker.com/r/arm32v7/adoptopenjdk/
    dockerBaseImage := "arm32v7/adoptopenjdk:11-jre-hotspot-bionic",
    dockerExposedPorts := Seq.empty[Int],
    dockerExposedUdpPorts := Seq.empty[Int],
    dockerAliases ++= Seq()
  )

// lazy val agentForARM = {
//   import com.typesafe.sbt.packager.docker._
//   agent.settings(
//     dockerCommands := Seq(
//       Cmd("FROM", "openjdk:8"),
//       Cmd("LABEL", s"""MAINTAINER="${maintainer.value}""""),
//       ExecCmd("CMD", "echo", "Hello, World from Docker")
//     )
//   )
// }

lazy val processor = (project in file("oracle-peak/processor"))
  .settings(sharedSettings: _*)
  .settings(name := "processor")
  .dependsOn(core)
