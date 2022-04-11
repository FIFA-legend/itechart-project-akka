package com.itechart.project.repository

import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.player.{LastName, Player, PlayerId}
import com.itechart.project.repository.slick_impl.SlickPlayerRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait PlayerRepository {
  def findAll: Future[List[Player]]
  def findById(id:             PlayerId):     Future[Option[Player]]
  def findByLastName(lastName: LastName):     Future[List[Player]]
  def findByCountry(countryId: CountryId):    Future[List[Player]]
  def create(player:           Player):       Future[PlayerId]
  def createAll(players:       List[Player]): Future[List[PlayerId]]
  def update(player:           Player):       Future[Int]
  def delete(id:               PlayerId):     Future[Int]
}

object PlayerRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): PlayerRepository =
    new SlickPlayerRepository(db)
}
