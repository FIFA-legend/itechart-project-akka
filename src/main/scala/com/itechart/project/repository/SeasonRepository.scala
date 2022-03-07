package com.itechart.project.repository

import com.itechart.project.domain.season.{Season, SeasonId, SeasonName}
import com.itechart.project.repository.slick_impl.SlickSeasonRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait SeasonRepository {
  def findAll: Future[List[Season]]
  def findById(id:       SeasonId):     Future[Option[Season]]
  def findByName(name:   SeasonName):   Future[Option[Season]]
  def create(season:     Season):       Future[SeasonId]
  def createAll(seasons: List[Season]): Future[List[SeasonId]]
  def update(season:     Season):       Future[Int]
  def delete(id:         SeasonId):     Future[Int]
}

object SeasonRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): SeasonRepository =
    new SlickSeasonRepository(db)
}
