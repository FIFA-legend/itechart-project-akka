package com.itechart.project.repository

import com.itechart.project.domain.football_match.{Match, MatchId}
import com.itechart.project.repository.slick_impl.SlickMatchRepository
import slick.jdbc.MySQLProfile

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait MatchRepository {
  def findAll: Future[List[Match]]
  def findById(id:          MatchId):   Future[Option[Match]]
  def findByDate(date:      LocalDate): Future[List[Match]]
  def create(footballMatch: Match):     Future[MatchId]
  def update(footballMatch: Match):     Future[Int]
  def delete(id:            MatchId):   Future[Int]
}

object MatchRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): MatchRepository =
    new SlickMatchRepository(db)
}
