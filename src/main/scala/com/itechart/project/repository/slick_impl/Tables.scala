package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.{Continent, Country, CountryCode, CountryId, CountryName}
import com.itechart.project.domain.formation.{Formation, FormationId, FormationName}
import com.itechart.project.domain.league.{League, LeagueId, LeagueName}
import com.itechart.project.domain.referee.{Referee, RefereeFirstName, RefereeId, RefereeImage, RefereeLastName}
import com.itechart.project.domain.season.{Season, SeasonId, SeasonName}
import com.itechart.project.domain.stage.{Stage, StageId, StageName}
import com.itechart.project.domain.team.{Team, TeamFullName, TeamId, TeamLogo, TeamShortName}
import com.itechart.project.domain.venue.{Capacity, Venue, VenueCity, VenueId, VenueName}
import com.itechart.project.repository.slick_impl.Implicits._
import slick.lifted.TableQuery
import slick.jdbc.MySQLProfile.api._

import java.sql.Date

object Tables {

  class CountryTable(tag: Tag) extends Table[Country](tag, None, "countries") {
    override def * = (id, name, countryCode, continent) <> (Country.tupled, Country.unapply)
    val id:          Rep[CountryId]   = column[CountryId]("id", O.AutoInc, O.PrimaryKey)
    val name:        Rep[CountryName] = column[CountryName]("name")
    val countryCode: Rep[CountryCode] = column[CountryCode]("code")
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
    val country = foreignKey("FK_leagues_countries", countryId, countryTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class RefereeTable(tag: Tag) extends Table[Referee](tag, None, "referees") {
    override def * = (id, firstName, lastName, image, countryId) <> (Referee.tupled, Referee.unapply)
    val id:        Rep[RefereeId]            = column[RefereeId]("id", O.AutoInc, O.PrimaryKey)
    val firstName: Rep[RefereeFirstName]     = column[RefereeFirstName]("first_name", O.Unique)
    val lastName:  Rep[RefereeLastName]      = column[RefereeLastName]("last_name", O.Unique)
    val image:     Rep[Option[RefereeImage]] = column[Option[RefereeImage]]("image", O.Unique)
    val countryId: Rep[CountryId]            = column[CountryId]("country_id")
    val country = foreignKey("FK_referees_countries", countryId, countryTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
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

  class StageTable(tag: Tag) extends Table[Stage](tag, None, "stages") {
    override def * = (id, name) <> (Stage.tupled, Stage.unapply)
    val id:   Rep[StageId]   = column[StageId]("id", O.AutoInc, O.PrimaryKey)
    val name: Rep[StageName] = column[StageName]("name")
  }

  class TeamTable(tag: Tag) extends Table[Team](tag, None, "teams") {
    override def * = (id, fullName, shortName, logo, countryId) <> (Team.tupled, Team.unapply)
    val id:        Rep[TeamId]           = column[TeamId]("id", O.AutoInc, O.PrimaryKey)
    val fullName:  Rep[TeamFullName]     = column[TeamFullName]("full_name")
    val shortName: Rep[TeamShortName]    = column[TeamShortName]("short_name")
    val logo:      Rep[Option[TeamLogo]] = column[Option[TeamLogo]]("logo")
    val countryId: Rep[CountryId]        = column[CountryId]("country_id")
    def country = foreignKey("FK_teams_countries", countryId, countryTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class VenueTable(tag: Tag) extends Table[Venue](tag, None, "venues") {
    override def * = (id, name, capacity, city, countryId) <> (Venue.tupled, Venue.unapply)
    val id:        Rep[VenueId]   = column[VenueId]("id", O.AutoInc, O.PrimaryKey)
    val name:      Rep[VenueName] = column[VenueName]("name")
    val capacity:  Rep[Capacity]  = column[Capacity]("capacity")
    val city:      Rep[VenueCity] = column[VenueCity]("city")
    val countryId: Rep[CountryId] = column[CountryId]("country_id")
    def country = foreignKey("FK_venues_countries", countryId, countryTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  val countryTable = TableQuery[CountryTable]

  val formationTable = TableQuery[FormationTable]

  val leagueTable = TableQuery[LeagueTable]

  val refereeTable = TableQuery[RefereeTable]

  val seasonTable = TableQuery[SeasonTable]

  val stageTable = TableQuery[StageTable]

  val teamTable = TableQuery[TeamTable]

  val venueTable = TableQuery[VenueTable]
}
