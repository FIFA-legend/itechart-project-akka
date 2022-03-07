package com.itechart.project
import com.itechart.project.configuration.ConfigurationTypes.DatabaseConfiguration
import com.itechart.project.configuration.DatabaseSettings
import com.itechart.project.domain.country.Continent.Europe
import com.itechart.project.domain.country.{Continent, Country, CountryId}
import com.itechart.project.repository.CountryRepository
import eu.timepit.refined.auto._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main {

  def main(args: Array[String]): Unit = {
    val databaseConfiguration = DatabaseConfiguration(
      "slick.jdbc.MySQLProfile",
      "com.mysql.cj.jdbc.Driver",
      "jdbc:mysql://localhost:3306/football_statistics?serverTimezone=Europe/Moscow",
      "root",
      "0987654321KnKn",
      "migration",
      "mysql"
    )
    val db                = DatabaseSettings.connection(databaseConfiguration)
    val countryRepository = CountryRepository.of(db)
    countryRepository
      .create(
        Country(
          CountryId(1L),
          "England",
          "en",
          Europe
        )
      )
      .onComplete {
        case Failure(exception) => println(exception)
        case Success(value)     => println(value)
      }
    // countryRepository.findAll.foreach(println)

    Thread.sleep(10000)
  }

}
