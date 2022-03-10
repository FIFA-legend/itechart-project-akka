package com.itechart.project.repository

import com.itechart.project.domain.player_stats.{PlayerStats, PlayerStatsId}
import com.itechart.project.repository.slick_impl.SlickPlayerStatsRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait PlayerStatsRepository {
  def findById(id:        PlayerStatsId): Future[Option[PlayerStats]]
  def create(playerStats: PlayerStats):   Future[PlayerStatsId]
  def update(playerStats: PlayerStats):   Future[Int]
  def delete(id:          PlayerStatsId): Future[Int]
}

object PlayerStatsRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): PlayerStatsRepository =
    new SlickPlayerStatsRepository(db)
}
