package com.itechart.project.repository

import com.itechart.project.domain.football_match.Match
import com.itechart.project.domain.player.Player
import com.itechart.project.domain.players_in_matches.PlayerInMatch
import com.itechart.project.repository.slick_impl.SlickPlayerInMatchRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait PlayerInMatchRepository {
  def findByPlayer(player:       Player):        Future[List[PlayerInMatch]]
  def findByMatch(footballMatch: Match):         Future[List[PlayerInMatch]]
  def create(playerInMatch:      PlayerInMatch): Future[Int]
  def delete(playerInMatch:      PlayerInMatch): Future[Int]
}
object PlayerInMatchRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): PlayerInMatchRepository =
    new SlickPlayerInMatchRepository(db)
}
