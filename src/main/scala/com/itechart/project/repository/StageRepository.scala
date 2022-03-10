package com.itechart.project.repository

import com.itechart.project.domain.stage.{Stage, StageId, StageName}
import com.itechart.project.repository.slick_impl.SlickStageRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait StageRepository {
  def findAll: Future[List[Stage]]
  def findById(id:      StageId):     Future[Option[Stage]]
  def findByName(name:  StageName):   Future[Option[Stage]]
  def create(stage:     Stage):       Future[StageId]
  def createAll(stages: List[Stage]): Future[List[StageId]]
  def update(stage:     Stage):       Future[Int]
  def delete(id:        StageId):     Future[Int]
}

object StageRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): StageRepository =
    new SlickStageRepository(db)
}
