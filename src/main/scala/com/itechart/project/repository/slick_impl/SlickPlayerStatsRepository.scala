package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.player_stats.{PlayerStats, PlayerStatsId}
import com.itechart.project.repository.PlayerStatsRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickPlayerStatsRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends PlayerStatsRepository {

  override def findById(id: PlayerStatsId): Future[Option[PlayerStats]] = {
    val playerStatsById = playerStatsTable.filter(_.id === id)
    db.run[Seq[PlayerStats]](playerStatsById.result).map(_.headOption)
  }

  override def create(playerStats: PlayerStats): Future[PlayerStatsId] = {
    val insertPlayerStatsQuery = (playerStatsTable returning playerStatsTable.map(_.id)) += playerStats
    db.run(insertPlayerStatsQuery)
  }

  override def update(playerStats: PlayerStats): Future[Int] = {
    def getValuesTuple(playerStats: PlayerStats) = {
      (
        playerStats.shirtNumber,
        playerStats.position,
        playerStats.startMinute,
        playerStats.playedMinutes,
        playerStats.goals,
        playerStats.assists,
        playerStats.successfulTackles,
        playerStats.totalTackles,
        playerStats.successfulPasses,
        playerStats.totalPasses,
        playerStats.successfulDribbling,
        playerStats.totalDribbling
      )
    }

    val updatePlayerStatsQuery = playerStatsTable
      .filter(_.id === playerStats.id)
      .map(stats =>
        (
          stats.shirtNumber,
          stats.position,
          stats.startMinute,
          stats.playedMinutes,
          stats.goals,
          stats.assists,
          stats.successfulTackles,
          stats.totalTackles,
          stats.successfulPasses,
          stats.totalPasses,
          stats.successfulDribbling,
          stats.totalDribbling
        )
      )
      .update(getValuesTuple(playerStats))
    db.run(updatePlayerStatsQuery)
  }

  override def delete(id: PlayerStatsId): Future[Int] = {
    val deletePlayerStatsQuery = playerStatsTable.filter(_.id === id).delete
    db.run(deletePlayerStatsQuery)
  }
}
