package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.formation.{Formation, FormationId, FormationName}
import com.itechart.project.repository.FormationRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickFormationRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends FormationRepository {

  override def findAll: Future[List[Formation]] = {
    val allFormationsQuery = formationTable.result
    db.run[Seq[Formation]](allFormationsQuery).map(_.toList)
  }

  override def findById(id: FormationId): Future[Option[Formation]] = {
    val formationByIdQuery = formationTable.filter(_.id === id)
    db.run[Seq[Formation]](formationByIdQuery.result).map(_.headOption)
  }

  override def findByName(name: FormationName): Future[Option[Formation]] = {
    val formationByNameQuery = formationTable.filter(_.name === name)
    db.run[Seq[Formation]](formationByNameQuery.result).map(_.headOption)
  }

  override def create(formation: Formation): Future[FormationId] = {
    val insertFormationQuery = (formationTable returning formationTable.map(_.id)) += formation
    db.run(insertFormationQuery)
  }

  override def createAll(formations: List[Formation]): Future[List[FormationId]] = {
    val insertFormationsQuery = (formationTable returning formationTable.map(_.id)) ++= formations
    db.run(insertFormationsQuery).map(_.toList)
  }

  override def update(formation: Formation): Future[Int] = {
    val updateFormationQuery = formationTable.filter(_.id === formation.id).map(_.name).update(formation.name)
    db.run(updateFormationQuery)
  }

  override def delete(id: FormationId): Future[Int] = {
    val deleteFormationQuery = formationTable.filter(_.id === id).delete
    db.run(deleteFormationQuery)
  }
}
