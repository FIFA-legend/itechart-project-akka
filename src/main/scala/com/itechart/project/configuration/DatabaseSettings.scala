package com.itechart.project.configuration

import com.itechart.project.configuration.ConfigurationTypes.DatabaseConfiguration
import org.flywaydb.core.Flyway

import java.io.File

object DatabaseSettings {

  class FlywayMigrator(configuration: DatabaseConfiguration) {
    def migrate(): Int = {
      val flyway = configureFlyway(configuration)
      flyway.migrate().migrationsExecuted
    }

    private def configureFlyway(configuration: DatabaseConfiguration): Flyway = {
      Flyway
        .configure()
        .dataSource(configuration.url, configuration.user, configuration.password)
        .locations(s"${configuration.migrationLocation}" + File.separator + s"${configuration.provider}")
        .load()
    }
  }

  def migrator(configuration: DatabaseConfiguration): FlywayMigrator =
    new FlywayMigrator(configuration)

}
