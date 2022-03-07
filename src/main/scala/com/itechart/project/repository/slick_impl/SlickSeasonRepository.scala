package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.season.{Season, SeasonId, SeasonName}
import com.itechart.project.repository.SeasonRepository
import com.itechart.project.utils.RefinedConversions.convertParameter
import eu.timepit.refined.auto._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcType, MySQLProfile}
import slick.jdbc.MySQLProfile.api._

import java.sql.Date
import scala.concurrent.{ExecutionContext, Future}

class SlickSeasonRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext) extends SeasonRepository {

  implicit private val seasonIdTypeMapper: JdbcType[SeasonId] with BaseTypedType[SeasonId] =
    MappedColumnType.base[SeasonId, Int](_.value, SeasonId)
  implicit private val seasonNameTypeMapper: JdbcType[SeasonName] with BaseTypedType[SeasonName] =
    MappedColumnType.base[SeasonName, String](_.value, convertParameter(_, "2021/2022"))

  class SeasonTable(tag: Tag) extends Table[Season](tag, None, "seasons") {
    override def * = (id, name, isCurrent, startDate, endDate) <> (Season.tupled, Season.unapply)
    val id:        Rep[SeasonId]   = column[SeasonId]("id", O.AutoInc, O.PrimaryKey)
    val name:      Rep[SeasonName] = column[SeasonName]("name")
    val isCurrent: Rep[Boolean]    = column[Boolean]("is_current")
    val startDate: Rep[Date]       = column[Date]("start_date")
    val endDate:   Rep[Date]       = column[Date]("end_date")
  }

  private val seasonTable = TableQuery[SeasonTable]

  override def findAll: Future[List[Season]] = {
    val allSeasonsQuery = seasonTable.result
    db.run[Seq[Season]](allSeasonsQuery).map(_.toList)
  }

  override def findById(id: SeasonId): Future[Option[Season]] = {
    val seasonByIdQuery = seasonTable.filter(_.id === id)
    db.run[Seq[Season]](seasonByIdQuery.result).map(_.headOption)
  }

  override def findByName(name: SeasonName): Future[Option[Season]] = {
    val seasonByNameQuery = seasonTable.filter(_.name === name)
    db.run[Seq[Season]](seasonByNameQuery.result).map(_.headOption)
  }

  override def create(season: Season): Future[SeasonId] = {
    val insertSeasonQuery = (seasonTable returning seasonTable.map(_.id)) += season
    db.run(insertSeasonQuery)
  }

  override def createAll(seasons: List[Season]): Future[List[SeasonId]] = {
    val insertSeasonsQuery = (seasonTable returning seasonTable.map(_.id)) ++= seasons
    db.run(insertSeasonsQuery).map(_.toList)
  }

  override def update(season: Season): Future[Int] = {
    val updateSeasonQuery = seasonTable
      .filter(_.id === season.id)
      .map(season => (season.name, season.isCurrent, season.startDate, season.startDate))
      .update((season.name, season.isCurrent, season.startDate, season.endDate))
    db.run(updateSeasonQuery)
  }

  override def delete(id: SeasonId): Future[Int] = {
    val deleteSeasonQuery = seasonTable.filter(_.id === id).delete
    db.run(deleteSeasonQuery)
  }
}
