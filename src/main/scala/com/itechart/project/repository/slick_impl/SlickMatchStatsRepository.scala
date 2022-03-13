package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.match_stats.{MatchStats, MatchStatsId}
import com.itechart.project.repository.MatchStatsRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickMatchStatsRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends MatchStatsRepository {

  override def findById(id: MatchStatsId): Future[Option[MatchStats]] = {
    val matchStatsById = matchStatsTable.filter(_.id === id)
    db.run[Seq[MatchStats]](matchStatsById.result).map(_.headOption)
  }

  override def create(matchStats: MatchStats): Future[MatchStatsId] = {
    val insertMatchStatsQuery = (matchStatsTable returning matchStatsTable.map(_.id)) += matchStats
    db.run(insertMatchStatsQuery)
  }

  override def update(matchStats: MatchStats): Future[Int] = {
    val updateMatchStatsQuery = matchStatsTable
      .filter(_.id === matchStats.id)
      .map(table =>
        (
          table.htHomeTeamScore,
          table.htAwayTeamScore,
          table.ftHomeTeamScore,
          table.ftAwayTeamScore,
          table.etHomeTeamScore,
          table.etAwayTeamScore,
          table.pHomeTeamScore,
          table.pAwayTeamScore,
          table.attendance
        )
      )
      .update(
        (
          matchStats.htHomeTeamScore,
          matchStats.htAwayTeamScore,
          matchStats.ftHomeTeamScore,
          matchStats.ftAwayTeamScore,
          matchStats.etHomeTeamScore,
          matchStats.etAwayTeamScore,
          matchStats.pHomeTeamScore,
          matchStats.pAwayTeamScore,
          matchStats.attendance
        )
      )
    db.run(updateMatchStatsQuery)
  }

  override def delete(id: MatchStatsId): Future[Int] = {
    val deleteMatchStatsQuery = matchStatsTable.filter(_.id === id).delete
    db.run(deleteMatchStatsQuery)
  }
}
