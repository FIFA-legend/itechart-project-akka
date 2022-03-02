package com.itechart.project.configuration

object ConfigurationTypes {

  final case class DatabaseConfiguration(
    provider:          String,
    driver:            String,
    url:               String,
    user:              String,
    password:          String,
    migrationLocation: String
  )

}
