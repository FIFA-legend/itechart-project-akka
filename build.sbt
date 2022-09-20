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

val akkaVersion            = "2.6.20"
val akkaHttpVersion        = "10.2.10"
val akkaPersistenceVersion = "3.5.3"
val circeVersion           = "0.14.1"
val circeConfigVersion     = "0.8.0"
val courierMailerVersion   = "3.2.0"
val enumeratumVersion      = "1.7.0"
val scalaTestVersion       = "3.2.9"
val mySqlVersion           = "8.0.28"
val jwtVersion             = "5.0.0"
val flywayVersion          = "8.5.4"
val refinedVersion         = "0.9.28"
val slickVersion           = "3.3.3"

libraryDependencies ++= Seq(
  // akka essentials
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
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
  "mysql" % "mysql-connector-java" % mySqlVersion,
  // database migrator
  "org.flywaydb" % "flyway-core"  % flywayVersion,
  "org.flywaydb" % "flyway-mysql" % flywayVersion,
  // Slick
  "com.typesafe.slick" %% "slick"          % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.typesafe.slick" %% "slick-codegen"  % slickVersion,
  // Type safety
  "eu.timepit" %% "refined" % refinedVersion,
  // Circe
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-config"  % circeConfigVersion,
  // enumeratum
  "com.beachape" %% "enumeratum" % enumeratumVersion,
  // mailer
  "com.github.daddykotex" %% "courier" % courierMailerVersion,
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
)

run / fork := true
