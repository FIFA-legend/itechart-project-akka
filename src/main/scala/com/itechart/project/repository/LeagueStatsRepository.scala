package com.itechart.project.repository

import com.itechart.project.domain.league.League
import com.itechart.project.domain.league_stats.LeagueStats
import com.itechart.project.domain.season.Season
import com.itechart.project.domain.team.Team
import com.itechart.project.repository.slick_impl.SlickLeagueStatsRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait LeagueStatsRepository {
  def findBySeasonAndLeagueAndTeam(season: Season, league: League, team: Team): Future[Option[LeagueStats]]
  def findBySeasonAndLeague(season:        Season, league: League): Future[List[LeagueStats]]
  def create(stats:                        LeagueStats): Future[Int]
  def update(stats:                        LeagueStats):   Future[Int]
  def delete(stats:                        LeagueStats):   Future[Int]
}

object LeagueStatsRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): LeagueStatsRepository =
    new SlickLeagueStatsRepository(db)
}
