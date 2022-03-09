package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.{Continent, Country, CountryCode, CountryId, CountryName}
import com.itechart.project.domain.formation.{Formation, FormationId, FormationName}
import com.itechart.project.domain.league.{League, LeagueId, LeagueName}
import com.itechart.project.domain.season.{Season, SeasonId, SeasonName}
import com.itechart.project.repository.slick_impl.Implicits._
import slick.lifted.TableQuery
import slick.jdbc.MySQLProfile.api._

import java.sql.Date

object Tables {

  class CountryTable(tag: Tag) extends Table[Country](tag, None, "countries") {
    override def * = (id, name, countryCode, continent) <> (Country.tupled, Country.unapply)
    val id:          Rep[CountryId]   = column[CountryId]("id", O.AutoInc, O.PrimaryKey)
    val name:        Rep[CountryName] = column[CountryName]("name")
    val countryCode: Rep[CountryCode] = column[CountryCode]("country_code")
    val continent:   Rep[Continent]   = column[Continent]("continent")
  }

  class FormationTable(tag: Tag) extends Table[Formation](tag, None, "formations") {
    override def * = (id, name) <> (Formation.tupled, Formation.unapply)
    val id:   Rep[FormationId]   = column[FormationId]("id", O.AutoInc, O.PrimaryKey)
    val name: Rep[FormationName] = column[FormationName]("name")
  }

  class LeagueTable(tag: Tag) extends Table[League](tag, None, "leagues") {
    override def * = (id, name, countryId) <> (League.tupled, League.unapply)
    val id:        Rep[LeagueId]   = column[LeagueId]("id", O.AutoInc, O.PrimaryKey)
    val name:      Rep[LeagueName] = column[LeagueName]("name", O.Unique)
    val countryId: Rep[CountryId]  = column[CountryId]("country_id")
    val country = foreignKey("FK_seasons_countries", countryId, countryTable)(
      _.id,
      onUpdate = ForeignKeyAction.Cascade,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class SeasonTable(tag: Tag) extends Table[Season](tag, None, "seasons") {
    override def * = (id, name, isCurrent, startDate, endDate) <> (Season.tupled, Season.unapply)
    val id:        Rep[SeasonId]   = column[SeasonId]("id", O.AutoInc, O.PrimaryKey)
    val name:      Rep[SeasonName] = column[SeasonName]("name")
    val isCurrent: Rep[Boolean]    = column[Boolean]("is_current")
    val startDate: Rep[Date]       = column[Date]("start_date")
    val endDate:   Rep[Date]       = column[Date]("end_date")
  }

  val countryTable = TableQuery[CountryTable]

  val formationTable = TableQuery[FormationTable]

  val leagueTable = TableQuery[LeagueTable]

  val seasonTable = TableQuery[SeasonTable]
}
