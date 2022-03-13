package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.league.League
import com.itechart.project.domain.league_stats.LeagueStats
import com.itechart.project.domain.season.Season
import com.itechart.project.domain.team.Team
import com.itechart.project.repository.LeagueStatsRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickLeagueStatsRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends LeagueStatsRepository {

  override def findBySeasonAndLeagueAndTeam(season: Season, league: League, team: Team): Future[Option[LeagueStats]] = {
    val matchStatsBySeasonAndLeagueAndTeam = leagueStatsTable
      .filter(table => table.seasonId === season.id && table.leagueId === league.id && table.teamId === team.id)
    db.run[Seq[LeagueStats]](matchStatsBySeasonAndLeagueAndTeam.result).map(_.headOption)
  }

  override def findBySeasonAndLeague(season: Season, league: League): Future[List[LeagueStats]] = {
    val matchStatsBySeasonAndLeague = leagueStatsTable
      .filter(table => table.seasonId === season.id && table.leagueId === league.id)
    db.run[Seq[LeagueStats]](matchStatsBySeasonAndLeague.result).map(_.toList)
  }

  override def create(stats: LeagueStats): Future[Int] = {
    val insertMatchStatsQuery = leagueStatsTable += stats
    db.run(insertMatchStatsQuery)
  }

  override def update(stats: LeagueStats): Future[Int] = {
    val updateLeagueStatsQuery = leagueStatsTable
      .filter(t => t.seasonId === stats.seasonId && t.leagueId === stats.leagueId && t.teamId === stats.teamId)
      .map(t => (t.place, t.points, t.scoredGoals, t.concededGoals, t.victories, t.defeats, t.draws))
      .update(
        (stats.place, stats.points, stats.scoredGoals, stats.concededGoals, stats.victories, stats.defeats, stats.draws)
      )
    db.run(updateLeagueStatsQuery)
  }

  override def delete(stats: LeagueStats): Future[Int] = {
    val deleteLeagueStatsQuery = leagueStatsTable
      .filter(t => t.seasonId === stats.seasonId && t.leagueId === stats.leagueId && t.teamId === stats.teamId)
      .delete
    db.run(deleteLeagueStatsQuery)
  }
}
