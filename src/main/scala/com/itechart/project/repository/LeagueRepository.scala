package com.itechart.project.repository

import com.itechart.project.domain.country.Country
import com.itechart.project.domain.league.{League, LeagueId, LeagueName}
import com.itechart.project.repository.slick_impl.SlickLeagueRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait LeagueRepository {
  def findAll: Future[List[League]]
  def findById(id:           LeagueId):     Future[Option[League]]
  def findByName(name:       LeagueName):   Future[Option[League]]
  def findByCountry(country: Country):      Future[List[League]]
  def create(league:         League):       Future[LeagueId]
  def createAll(leagues:     List[League]): Future[List[LeagueId]]
  def update(league:         League):       Future[Int]
  def delete(id:             LeagueId):     Future[Int]
}

object LeagueRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): LeagueRepository =
    new SlickLeagueRepository(db)
}
