package com.itechart.project.configuration

import io.circe.generic.JsonCodec

object ConfigurationTypes {

  @JsonCodec
  final case class DatabaseConfiguration(
    provider:          String,
    driver:            String,
    url:               String,
    user:              String,
    password:          String,
    migrationLocation: String,
    configurationName: String
  )

}
