package com.itechart.project.repository

import com.itechart.project.domain.formation.{Formation, FormationId, FormationName}
import com.itechart.project.repository.slick_impl.SlickFormationRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait FormationRepository {
  def findAll: Future[List[Formation]]
  def findById(id:          FormationId):     Future[Option[Formation]]
  def findByName(name:      FormationName):   Future[Option[Formation]]
  def create(formation:     Formation):       Future[FormationId]
  def createAll(formations: List[Formation]): Future[List[FormationId]]
  def update(formation:     Formation):       Future[Int]
  def delete(id:            FormationId):     Future[Int]
}

object FormationRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): FormationRepository =
    new SlickFormationRepository(db)
}
