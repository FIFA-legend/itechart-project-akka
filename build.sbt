enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

name := "FootballStats"

version := "0.1"

scalaVersion := "2.13.8"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Ymacro-annotations",
  "-Xfatal-warnings"
)

ThisBuild / scalafmtOnCompile := true

lazy val root = (project in file("."))
  .settings(
    name := "akka-project"
  )

val akkaVersion            = "2.6.18"
val akkaHttpVersion        = "10.2.8"
val akkaPersistenceVersion = "3.5.3"
val scalaTestVersion       = "3.2.9"
val mySqlVersion           = "8.0.28"
val jwtVersion             = "5.0.0"
val flywayVersion          = "8.5.0"
val refinedVersion         = "0.9.28"

libraryDependencies ++= Seq(
  // akka essentials
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  // akka persistence
  "com.typesafe.akka"   %% "akka-persistence"      % akkaVersion,
  "com.github.dnvriend" %% "akka-persistence-jdbc" % akkaPersistenceVersion,
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion,
  // testing
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest"     %% "scalatest"    % scalaTestVersion,
  // JWT
  "com.pauldijou" %% "jwt-spray-json" % jwtVersion,
  // MySQL connector
  "mysql"        % "mysql-connector-java" % mySqlVersion,
  "org.flywaydb" % "flyway-core"          % flywayVersion,
  // Type safety
  "eu.timepit" %% "refined" % refinedVersion,
)
