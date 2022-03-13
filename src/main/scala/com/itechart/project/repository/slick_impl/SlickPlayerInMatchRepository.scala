package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.football_match.Match
import com.itechart.project.domain.player.Player
import com.itechart.project.domain.players_in_matches.PlayerInMatch
import com.itechart.project.repository.PlayerInMatchRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickPlayerInMatchRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends PlayerInMatchRepository {

  override def findByPlayer(player: Player): Future[List[PlayerInMatch]] = {
    val playerInMatchesByPlayerQuery = playerInMatchTable.filter(_.playerId === player.id)
    db.run[Seq[PlayerInMatch]](playerInMatchesByPlayerQuery.result).map(_.toList)
  }

  override def findByMatch(footballMatch: Match): Future[List[PlayerInMatch]] = {
    val playerInMatchesByMatchQuery = playerInMatchTable.filter(_.matchId === footballMatch.id)
    db.run[Seq[PlayerInMatch]](playerInMatchesByMatchQuery.result).map(_.toList)
  }

  override def create(playerInMatch: PlayerInMatch): Future[Int] = {
    val insertPlayerInMatchQuery = playerInMatchTable += playerInMatch
    db.run(insertPlayerInMatchQuery)
  }

  override def delete(playerInMatch: PlayerInMatch): Future[Int] = {
    val deletePlayerInMatchQuery = playerInMatchTable
      .filter(table => table.playerId === playerInMatch.playerId && table.matchId === playerInMatch.matchId)
      .delete
    db.run(deletePlayerInMatchQuery)
  }
}
