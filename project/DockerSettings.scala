import sbt._, Keys._, sbt.nio.Keys._
// import sbt.{ Def, Resolver, _ }
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbtbuildinfo.BuildInfoKeys._

object DockerSettings {
  lazy val sharedDockerSettings = Seq(
    mainClass in(Compile, packageBin) := Some("com.pinkstack.oraclepeak.agent.Agent"),
    maintainer in Docker := "Oto Brglez - <otobrglez@gmail.com>",

    dockerRepository := Some("ghcr.io"),
    dockerUsername := Some("pinkstack"),
    dockerUpdateLatest := false,

    dockerExposedPorts := Seq.empty[Int],
    dockerExposedUdpPorts := Seq.empty[Int]
  )

  lazy val commonDockerCommands = Seq(
    Cmd("LABEL", "maintainer Oto Brglez <otobrglez@gmail.com>"),
    Cmd("LABEL", "org.opencontainers.image.url https://github.com/pinkstack/oracle-peak"),
    Cmd("LABEL", "org.opencontainers.image.source https://github.com/pinkstack/oracle-peak")
  )

  lazy val defaultArchSettings = sharedDockerSettings ++ Seq(
    packageName in Docker := "oracle-peak-agent",
    dockerBaseImage := "azul/zulu-openjdk-alpine:11-jre",
    dockerCommands := dockerCommands.value.flatMap {
      case add@Cmd("RUN", args@_*) if args.contains("id") =>
        commonDockerCommands ++ List(
          Cmd("RUN", "apk add --no-cache bash"),
          Cmd("ENV", "SBT_VERSION", sbtVersion.value),
          Cmd("ENV", "SCALA_VERSION", scalaVersion.value),
          Cmd("ENV", "ORACLE_PEAK_VERSION", version.value),
          add
        )
      case other => List(other)
    },
    dockerAliases ++= Seq(
      dockerAlias.value.withName("oracle-peak-agent")
    )
  )

  lazy val armV7DockerSettings = sharedDockerSettings ++ Seq(
    packageName in Docker := "oracle-peak-agent-arm32v7",
    dockerBaseImage := "arm32v7/adoptopenjdk:11-jre-hotspot-bionic",
    dockerCommands := dockerCommands.value.flatMap {
      case add@Cmd("RUN", args@_*) if args.contains("id") =>
        commonDockerCommands ++ List(
          Cmd("ENV", "SBT_VERSION", sbtVersion.value),
          Cmd("ENV", "SCALA_VERSION", scalaVersion.value),
          Cmd("ENV", "ORACLE_PEAK_VERSION", version.value),
          add
        )
      case other => List(other)
    },
    dockerAliases ++= Seq(
      dockerAlias.value.withName("oracle-peak-agent-arm32v7")
    )
  )
}

