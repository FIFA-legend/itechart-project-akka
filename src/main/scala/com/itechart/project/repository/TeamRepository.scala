package com.itechart.project.repository

import com.itechart.project.domain.country.Country
import com.itechart.project.domain.team.{Team, TeamFullName, TeamId}
import com.itechart.project.repository.slick_impl.SlickTeamRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait TeamRepository {
  def findAll: Future[List[Team]]
  def findById(id:           TeamId):       Future[Option[Team]]
  def findByName(name:       TeamFullName): Future[List[Team]]
  def findByCountry(country: Country):      Future[List[Team]]
  def create(team:           Team):         Future[TeamId]
  def createAll(teams:       List[Team]):   Future[List[TeamId]]
  def update(team:           Team):         Future[Int]
  def delete(id:             TeamId):       Future[Int]
}

object TeamRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): TeamRepository =
    new SlickTeamRepository(db)
}
