package com.itechart.project.repository

import com.itechart.project.domain.country.{Country, CountryId}
import com.itechart.project.repository.slick_impl.SlickCountryRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait CountryRepository {
  def findAll: Future[List[Country]]
  def findById(id:         CountryId):     Future[Option[Country]]
  def create(country:      Country):       Future[CountryId]
  def createAll(countries: List[Country]): Future[List[CountryId]]
  def update(country:      Country):       Future[Int]
  def delete(id:           CountryId):     Future[Int]
}

object CountryRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): CountryRepository =
    new SlickCountryRepository(db)
}
