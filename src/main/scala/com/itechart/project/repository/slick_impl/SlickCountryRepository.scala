package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country._
import com.itechart.project.repository.CountryRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickCountryRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends CountryRepository {

  override def findAll: Future[List[Country]] = {
    val allCountriesQuery = countryTable.result
    db.run[Seq[Country]](allCountriesQuery).map(_.toList)
  }

  override def findById(id: CountryId): Future[Option[Country]] = {
    val countryByIdQuery = countryTable.filter(_.id === id)
    db.run[Seq[Country]](countryByIdQuery.result).map(_.headOption)
  }

  override def create(country: Country): Future[CountryId] = {
    val insertCountryQuery = (countryTable returning countryTable.map(_.id)) += country
    db.run(insertCountryQuery)
  }

  override def createAll(countries: List[Country]): Future[List[CountryId]] = {
    val insertCountriesQuery = (countryTable returning countryTable.map(_.id)) ++= countries
    db.run(insertCountriesQuery).map(_.toList)
  }

  override def update(country: Country): Future[Int] = {
    val updateCountryQuery = countryTable
      .filter(_.id === country.id)
      .map(country => (country.name, country.countryCode, country.continent))
      .update((country.name, country.countryCode, country.continent))
    db.run(updateCountryQuery)
  }

  override def delete(id: CountryId): Future[Int] = {
    val deleteCountryQuery = countryTable.filter(_.id === id).delete
    db.run(deleteCountryQuery)
  }
}
