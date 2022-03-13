package com.itechart.project.repository

import com.itechart.project.domain.match_stats.{MatchStats, MatchStatsId}
import com.itechart.project.repository.slick_impl.SlickMatchStatsRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait MatchStatsRepository {
  def findById(id:       MatchStatsId): Future[Option[MatchStats]]
  def create(matchStats: MatchStats):   Future[MatchStatsId]
  def update(matchStats: MatchStats):   Future[Int]
  def delete(id:         MatchStatsId): Future[Int]
}

object MatchStatsRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): MatchStatsRepository =
    new SlickMatchStatsRepository(db)
}
