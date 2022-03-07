package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.formation.{Formation, FormationId, FormationName}
import com.itechart.project.repository.FormationRepository
import slick.ast.BaseTypedType
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.{JdbcType, MySQLProfile}
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

class SlickFormationRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends FormationRepository {

  implicit private val formationIdTypeMapper: JdbcType[FormationId] with BaseTypedType[FormationId] =
    MappedColumnType.base[FormationId, Int](_.value, FormationId)
  implicit private val formationNameTypeMapper: JdbcType[FormationName] with BaseTypedType[FormationName] =
    MappedColumnType.base[FormationName, String](
      {
        case FormationName.FOUR_THREE_THREE      => "4-3-3"
        case FormationName.FOUR_FOUR_TWO         => "4-4-2"
        case FormationName.FOUR_FOUR_ONE_ONE     => "4-4-1-1"
        case FormationName.FOUR_TWO_THREE_ONE    => "4-2-3-1"
        case FormationName.FOUR_THREE_ONE_TWO    => "4-3-1-2"
        case FormationName.FOUR_THREE_TWO_ONE    => "4-3-2-1"
        case FormationName.FIVE_THREE_TWO        => "5-3-2"
        case FormationName.THREE_FIVE_TWO        => "3-5-2"
        case FormationName.THREE_THREE_THREE_ONE => "3-3-3-1"
        case FormationName.FIVE_FOUR_ONE         => "5-4-1"
        case FormationName.THREE_FOUR_THREE      => "3-4-3"
        case FormationName.FOUR_TWO_TWO_TWO      => "4-2-2-2"
        case FormationName.FOUR_ONE_TWO_ONE_TWO  => "4-1-2-1-2"
        case FormationName.FOUR_TWO_ONE_THREE    => "4-2-1-3"
      },
      {
        case "4-3-3"     => FormationName.FOUR_THREE_THREE
        case "4-4-2"     => FormationName.FOUR_FOUR_TWO
        case "4-4-1-1"   => FormationName.FOUR_FOUR_ONE_ONE
        case "4-2-3-1"   => FormationName.FOUR_TWO_THREE_ONE
        case "4-3-1-2"   => FormationName.FOUR_THREE_ONE_TWO
        case "4-3-2-1"   => FormationName.FOUR_THREE_TWO_ONE
        case "5-3-2"     => FormationName.FIVE_THREE_TWO
        case "3-5-2"     => FormationName.THREE_FIVE_TWO
        case "3-3-3-1"   => FormationName.THREE_THREE_THREE_ONE
        case "5-4-1"     => FormationName.FIVE_FOUR_ONE
        case "3-4-3"     => FormationName.THREE_FOUR_THREE
        case "4-2-2-2"   => FormationName.FOUR_TWO_TWO_TWO
        case "4-1-2-1-2" => FormationName.FOUR_ONE_TWO_ONE_TWO
        case "4-2-1-3"   => FormationName.FOUR_TWO_ONE_THREE
      }
    )

  class FormationTable(tag: Tag) extends Table[Formation](tag, None, "formations") {
    override def * = (id, name) <> (Formation.tupled, Formation.unapply)
    val id:   Rep[FormationId]   = column[FormationId]("id", O.AutoInc, O.PrimaryKey)
    val name: Rep[FormationName] = column[FormationName]("name")
  }

  private val formationTable = TableQuery[FormationTable]

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
