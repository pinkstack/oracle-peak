import Dependencies._
import Settings._
import sbt.Keys.resolvers
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._

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
    packageName in Docker := "oracle-peak-agent-arm32v7",
    dockerUpdateLatest := false,
    // Read: https://hub.docker.com/r/arm32v7/adoptopenjdk/
    dockerBaseImage := "arm32v7/adoptopenjdk:11-jre-hotspot-bionic",
    dockerExposedPorts := Seq.empty[Int],
    dockerExposedUdpPorts := Seq.empty[Int],

    /*
    dockerCommands := dockerCommands.value.flatMap {
      case add@Cmd("RUN", args@_*) if args.contains("id") =>
        List(
          Cmd("LABEL", "maintainer Oto Brglez <otobrglez@gmail.com>"),
          Cmd("LABEL", "org.opencontainers.image.url https://github.com/pinkstack/oracle-peak"),
          Cmd("LABEL", "org.opencontainers.image.source https://github.com/pinkstack/oracle-peak"),
          Cmd("ENV", "SBT_VERSION", sbtVersion.value),
          Cmd("ENV", "SCALA_VERSION", scalaVersion.value),
          Cmd("ENV", "ORACLE_PEAK_VERSION", version.value),
          add
        )
      case other => List(other)
    },
   
     */
    dockerAliases ++= Seq(
      dockerAlias.value.withRegistryHost(Option("ghcr.io"))
        .withUsername(Option("pinkstack"))
        .withName("oracle-peak-agent-arm32v7")
        .withTag(Option(version.value))
    )

  )

lazy val processor = (project in file("oracle-peak/processor"))
  .settings(sharedSettings: _*)
  .settings(name := "processor")
  .dependsOn(core)
  .settings(libraryDependencies ++=
    Dependencies.neo4j
  )

// publishTo in ThisBuild := false
publishArtifact := false
skip in publish := true