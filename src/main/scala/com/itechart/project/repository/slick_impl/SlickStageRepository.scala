package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.stage.{Stage, StageId, StageName}
import com.itechart.project.repository.StageRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickStageRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext) extends StageRepository {

  override def findAll: Future[List[Stage]] = {
    val allStages = stageTable.result
    db.run[Seq[Stage]](allStages).map(_.toList)
  }

  override def findById(id: StageId): Future[Option[Stage]] = {
    val stageByIdQuery = stageTable.filter(_.id === id)
    db.run[Seq[Stage]](stageByIdQuery.result).map(_.headOption)
  }

  override def findByName(name: StageName): Future[Option[Stage]] = {
    val stageByNameQuery = stageTable.filter(_.name === name)
    db.run[Seq[Stage]](stageByNameQuery.result).map(_.headOption)
  }

  override def create(stage: Stage): Future[StageId] = {
    val insertStageQuery = (stageTable returning stageTable.map(_.id)) += stage
    db.run(insertStageQuery)
  }

  override def createAll(stages: List[Stage]): Future[List[StageId]] = {
    val insertStagesQuery = (stageTable returning stageTable.map(_.id)) ++= stages
    db.run(insertStagesQuery).map(_.toList)
  }

  override def update(stage: Stage): Future[Int] = {
    val updateStageQuery = stageTable.filter(_.id === stage.id).map(_.name).update(stage.name)
    db.run(updateStageQuery)
  }

  override def delete(id: StageId): Future[Int] = {
    val deleteStageQuery = stageTable.filter(_.id === id).delete
    db.run(deleteStageQuery)
  }
}
