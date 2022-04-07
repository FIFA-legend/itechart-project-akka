package com.itechart.project.repository

import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.venue.{Venue, VenueCity, VenueId, VenueName}
import com.itechart.project.repository.slick_impl.SlickVenueRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait VenueRepository {
  def findAll: Future[List[Venue]]
  def findById(id:             VenueId):     Future[Option[Venue]]
  def findByName(name:         VenueName):   Future[Option[Venue]]
  def findByCity(city:         VenueCity):   Future[List[Venue]]
  def findByCountry(countryId: CountryId):   Future[List[Venue]]
  def create(venue:            Venue):       Future[VenueId]
  def createAll(venues:        List[Venue]): Future[List[VenueId]]
  def update(venue:            Venue):       Future[Int]
  def delete(id:               VenueId):     Future[Int]
}

object VenueRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): VenueRepository =
    new SlickVenueRepository(db)
}
