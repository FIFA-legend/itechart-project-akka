package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.referee.{Referee, RefereeId}
import com.itechart.project.repository.RefereeRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickRefereeRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends RefereeRepository {

  override def findAll: Future[List[Referee]] = {
    val allRefereesQuery = refereeTable.result
    db.run[Seq[Referee]](allRefereesQuery).map(_.toList)
  }

  override def findById(id: RefereeId): Future[Option[Referee]] = {
    val refereeByIdQuery = refereeTable.filter(_.id === id)
    db.run[Seq[Referee]](refereeByIdQuery.result).map(_.headOption)
  }

  override def findByCountry(countryId: CountryId): Future[List[Referee]] = {
    val refereesByCountryQuery = refereeTable.filter(_.countryId === countryId)
    db.run[Seq[Referee]](refereesByCountryQuery.result).map(_.toList)
  }

  override def create(referee: Referee): Future[RefereeId] = {
    val insertRefereeQuery = (refereeTable returning refereeTable.map(_.id)) += referee
    db.run(insertRefereeQuery)
  }

  override def createAll(referees: List[Referee]): Future[List[RefereeId]] = {
    val insertRefereesQuery = (refereeTable returning refereeTable.map(_.id)) ++= referees
    db.run(insertRefereesQuery).map(_.toList)
  }

  override def update(referee: Referee): Future[Int] = {
    val updateRefereeQuery = refereeTable
      .filter(_.id === referee.id)
      .map(referee => (referee.firstName, referee.lastName, referee.image, referee.countryId))
      .update((referee.firstName, referee.lastName, referee.image, referee.countryId))
    db.run(updateRefereeQuery)
  }

  override def delete(id: RefereeId): Future[Int] = {
    val deleteCountryQuery = refereeTable.filter(_.id === id).delete
    db.run(deleteCountryQuery)
  }
}
