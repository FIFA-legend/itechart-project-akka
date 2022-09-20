package com.itechart.project.configuration

import com.itechart.project.configuration.ConfigurationTypes.DatabaseConfiguration
import org.flywaydb.core.Flyway
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

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
        .locations(s"src/main/resources/${configuration.migrationLocation}/${configuration.provider}")
        .baselineOnMigrate(true)
        .load()
    }
  }

  def migrator(configuration: DatabaseConfiguration): FlywayMigrator =
    new FlywayMigrator(configuration)

  def connection(configuration: DatabaseConfiguration): MySQLProfile.backend.Database =
    Database.forConfig(configuration.configurationName)

}
