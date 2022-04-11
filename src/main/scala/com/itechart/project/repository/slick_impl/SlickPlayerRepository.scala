package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.player.{LastName, Player, PlayerId}
import com.itechart.project.repository.PlayerRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickPlayerRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext) extends PlayerRepository {

  override def findAll: Future[List[Player]] = {
    val allPlayersQuery = playerTable.result
    db.run[Seq[Player]](allPlayersQuery).map(_.toList)
  }

  override def findById(id: PlayerId): Future[Option[Player]] = {
    val playerByIdQuery = playerTable.filter(_.id === id)
    db.run[Seq[Player]](playerByIdQuery.result).map(_.headOption)
  }

  override def findByLastName(lastName: LastName): Future[List[Player]] = {
    val playersByLastNameQuery = playerTable.filter(_.lastName === lastName)
    db.run[Seq[Player]](playersByLastNameQuery.result).map(_.toList)
  }

  override def findByCountry(countryId: CountryId): Future[List[Player]] = {
    val playersByCountryQuery = playerTable.filter(_.countryId === countryId)
    db.run[Seq[Player]](playersByCountryQuery.result).map(_.toList)
  }

  override def create(player: Player): Future[PlayerId] = {
    val insertPlayerQuery = (playerTable returning playerTable.map(_.id)) += player
    db.run(insertPlayerQuery)
  }

  override def createAll(players: List[Player]): Future[List[PlayerId]] = {
    val insertPlayersQuery = (playerTable returning playerTable.map(_.id)) ++= players
    db.run(insertPlayersQuery).map(_.toList)
  }

  override def update(player: Player): Future[Int] = {
    val updatePlayerQuery = playerTable
      .filter(_.id === player.id)
      .map(player =>
        (
          player.firstName,
          player.lastName,
          player.birthday,
          player.age,
          player.weight,
          player.height,
          player.image,
          player.countryId
        )
      )
      .update(
        (
          player.firstName,
          player.lastName,
          player.birthday,
          player.age,
          player.weight,
          player.height,
          player.image,
          player.countryId
        )
      )
    db.run(updatePlayerQuery)
  }

  override def delete(id: PlayerId): Future[Int] = {
    val deletePlayerQuery = playerTable.filter(_.id === id).delete
    db.run(deletePlayerQuery)
  }
}
