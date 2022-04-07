package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.venue.{Venue, VenueCity, VenueId, VenueName}
import com.itechart.project.repository.VenueRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickVenueRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext) extends VenueRepository {

  override def findAll: Future[List[Venue]] = {
    val allVenuesQuery = venueTable.result
    db.run[Seq[Venue]](allVenuesQuery).map(_.toList)
  }

  override def findById(id: VenueId): Future[Option[Venue]] = {
    val venueByIdQuery = venueTable.filter(_.id === id)
    db.run[Seq[Venue]](venueByIdQuery.result).map(_.headOption)
  }

  override def findByName(name: VenueName): Future[Option[Venue]] = {
    val venueByNameQuery = venueTable.filter(_.name === name)
    db.run[Seq[Venue]](venueByNameQuery.result).map(_.headOption)
  }

  override def findByCity(city: VenueCity): Future[List[Venue]] = {
    val venuesByCityQuery = venueTable.filter(_.city === city)
    db.run[Seq[Venue]](venuesByCityQuery.result).map(_.toList)
  }

  override def findByCountry(countryId: CountryId): Future[List[Venue]] = {
    val venuesByCountryQuery = venueTable.filter(_.countryId === countryId)
    db.run[Seq[Venue]](venuesByCountryQuery.result).map(_.toList)
  }

  override def create(venue: Venue): Future[VenueId] = {
    val insertVenueQuery = (venueTable returning venueTable.map(_.id)) += venue
    db.run(insertVenueQuery)
  }

  override def createAll(venues: List[Venue]): Future[List[VenueId]] = {
    val insertVenuesQuery = (venueTable returning venueTable.map(_.id)) ++= venues
    db.run(insertVenuesQuery).map(_.toList)
  }

  override def update(venue: Venue): Future[Int] = {
    val updateVenueQuery = venueTable
      .filter(_.id === venue.id)
      .map(venue => (venue.name, venue.capacity, venue.city, venue.countryId))
      .update((venue.name, venue.capacity, venue.city, venue.countryId))
    db.run(updateVenueQuery)
  }

  override def delete(id: VenueId): Future[Int] = {
    val deleteVenueQuery = venueTable.filter(_.id === id).delete
    db.run(deleteVenueQuery)
  }
}
