package com.itechart.project.repository

import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.referee.{Referee, RefereeId}
import com.itechart.project.repository.slick_impl.SlickRefereeRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait RefereeRepository {
  def findAll: Future[List[Referee]]
  def findById(id:             RefereeId):     Future[Option[Referee]]
  def findByCountry(countryId: CountryId):     Future[List[Referee]]
  def create(referee:          Referee):       Future[RefereeId]
  def createAll(referees:      List[Referee]): Future[List[RefereeId]]
  def update(referee:          Referee):       Future[Int]
  def delete(id:               RefereeId):     Future[Int]
}

object RefereeRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): RefereeRepository =
    new SlickRefereeRepository(db)
}
