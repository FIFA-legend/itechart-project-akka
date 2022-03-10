package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.Country
import com.itechart.project.domain.team.{Team, TeamFullName, TeamId}
import com.itechart.project.repository.TeamRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickTeamRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext) extends TeamRepository {

  override def findAll: Future[List[Team]] = {
    val allTeamsQuery = teamTable.result
    db.run[Seq[Team]](allTeamsQuery).map(_.toList)
  }

  override def findById(id: TeamId): Future[Option[Team]] = {
    val teamByIdQuery = teamTable.filter(_.id === id)
    db.run[Seq[Team]](teamByIdQuery.result).map(_.headOption)
  }

  override def findByName(name: TeamFullName): Future[List[Team]] = {
    val teamsByNameQuery = teamTable.filter(_.fullName === name)
    db.run[Seq[Team]](teamsByNameQuery.result).map(_.toList)
  }

  override def findByCountry(country: Country): Future[List[Team]] = {
    val teamsByCountryQuery = teamTable.filter(_.countryId === country.id)
    db.run[Seq[Team]](teamsByCountryQuery.result).map(_.toList)
  }

  override def create(team: Team): Future[TeamId] = {
    val insertTeamQuery = (teamTable returning teamTable.map(_.id)) += team
    db.run(insertTeamQuery)
  }

  override def createAll(teams: List[Team]): Future[List[TeamId]] = {
    val insertTeamsQuery = (teamTable returning teamTable.map(_.id)) ++= teams
    db.run(insertTeamsQuery).map(_.toList)
  }

  override def update(team: Team): Future[Int] = {
    val updateTeamQuery = teamTable
      .filter(_.id === team.id)
      .map(team => (team.fullName, team.shortName, team.logo, team.countryId))
      .update((team.name, team.shortCode, team.logo, team.countryId))
    db.run(updateTeamQuery)
  }

  override def delete(id: TeamId): Future[Int] = {
    val deleteTeamQuery = teamTable.filter(_.id === id).delete
    db.run(deleteTeamQuery)
  }
}
