package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.Country
import com.itechart.project.domain.league.{League, LeagueId, LeagueName}
import com.itechart.project.repository.LeagueRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickLeagueRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext) extends LeagueRepository {

  override def findAll: Future[List[League]] = {
    val allLeaguesQuery = leagueTable.result
    db.run[Seq[League]](allLeaguesQuery).map(_.toList)
  }

  override def findById(id: LeagueId): Future[Option[League]] = {
    val leagueByIdQuery = leagueTable.filter(_.id === id)
    db.run[Seq[League]](leagueByIdQuery.result).map(_.headOption)
  }

  override def findByName(name: LeagueName): Future[Option[League]] = {
    val leagueByNameQuery = leagueTable.filter(_.name === name)
    db.run[Seq[League]](leagueByNameQuery.result).map(_.headOption)
  }

  override def findByCountry(country: Country): Future[List[League]] = {
    val leaguesByCountryQuery = leagueTable.filter(_.countryId === country.id)
    db.run[Seq[League]](leaguesByCountryQuery.result).map(_.toList)
  }

  override def create(league: League): Future[LeagueId] = {
    val insertLeagueQuery = (leagueTable returning leagueTable.map(_.id)) += league
    db.run(insertLeagueQuery)
  }

  override def createAll(leagues: List[League]): Future[List[LeagueId]] = {
    val insertLeaguesQuery = (leagueTable returning leagueTable.map(_.id)) ++= leagues
    db.run(insertLeaguesQuery).map(_.toList)
  }

  override def update(league: League): Future[Int] = {
    val updateLeagueQuery = leagueTable
      .filter(_.id === league.id)
      .map(league => (league.name, league.countryId))
      .update((league.name, league.countryId))
    db.run(updateLeagueQuery)
  }

  override def delete(id: LeagueId): Future[Int] = {
    val deleteCountryQuery = leagueTable.filter(_.id === id).delete
    db.run(deleteCountryQuery)
  }
}
