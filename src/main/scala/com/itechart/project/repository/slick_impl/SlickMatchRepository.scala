package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.football_match.{Match, MatchId}
import com.itechart.project.repository.MatchRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickMatchRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext) extends MatchRepository {

  override def findAll: Future[List[Match]] = {
    val allMatchesQuery = matchTable.result
    db.run[Seq[Match]](allMatchesQuery).map(_.toList)
  }

  override def findById(id: MatchId): Future[Option[Match]] = {
    val matchByIdQuery = matchTable.filter(_.id === id)
    db.run[Seq[Match]](matchByIdQuery.result).map(_.headOption)
  }

  override def create(footballMatch: Match): Future[MatchId] = {
    val insertMatchQuery = (matchTable returning matchTable.map(_.id)) += footballMatch
    db.run(insertMatchQuery)
  }

  override def update(footballMatch: Match): Future[Int] = {
    val updateMatchQuery = matchTable
      .filter(_.id === footballMatch.id)
      .map(t =>
        (
          t.seasonId,
          t.leagueId,
          t.stageId,
          t.status,
          t.startDate,
          t.startTime,
          t.homeTeamId,
          t.awayTeamId,
          t.venueId,
          t.refereeId,
          t.matchStatsId,
          t.homeTeamFormationId,
          t.awayTeamFormationId
        )
      )
      .update(
        (
          footballMatch.seasonId,
          footballMatch.leagueId,
          footballMatch.stageId,
          footballMatch.status,
          footballMatch.startDate,
          footballMatch.startTime,
          footballMatch.homeTeamId,
          footballMatch.awayTeamId,
          footballMatch.venueId,
          footballMatch.refereeId,
          footballMatch.matchStatsId,
          footballMatch.homeTeamFormationId,
          footballMatch.awayTeamFormationId
        )
      )
    db.run(updateMatchQuery)
  }

  override def delete(id: MatchId): Future[Int] = {
    val deleteMatchQuery = matchTable.filter(_.id === id).delete
    db.run(deleteMatchQuery)
  }
}
