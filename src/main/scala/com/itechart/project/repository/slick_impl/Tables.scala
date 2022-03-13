package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.{Continent, Country, CountryCode, CountryId, CountryName}
import com.itechart.project.domain.formation.{Formation, FormationId, FormationName}
import com.itechart.project.domain.league.{League, LeagueId, LeagueName}
import com.itechart.project.domain.match_stats.{Attendance, MatchScore, MatchStats, MatchStatsId}
import com.itechart.project.domain.player.{Age, FirstName, Height, LastName, Player, PlayerId, PlayerImage, Weight}
import com.itechart.project.domain.player_stats.{
  Assists,
  Dribbling,
  Goals,
  Minute,
  Passes,
  PlayerStats,
  PlayerStatsId,
  Position,
  ShirtNumber,
  Tackles
}
import com.itechart.project.domain.referee.{Referee, RefereeFirstName, RefereeId, RefereeImage, RefereeLastName}
import com.itechart.project.domain.season.{Season, SeasonId, SeasonName}
import com.itechart.project.domain.stage.{Stage, StageId, StageName}
import com.itechart.project.domain.team.{Team, TeamFullName, TeamId, TeamLogo, TeamShortName}
import com.itechart.project.domain.user.{Email, Login, PasswordHash, Role, User, UserId}
import com.itechart.project.domain.user_subscriptions.{UserSubscriptionOnPlayer, UserSubscriptionOnTeam}
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
    def country = foreignKey("FK_leagues_countries", countryId, countryTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class MatchStatsTable(tag: Tag) extends Table[MatchStats](tag, None, "match_stats") {
    override def * = (
      id,
      htHomeTeamScore,
      htAwayTeamScore,
      ftHomeTeamScore,
      ftAwayTeamScore,
      etHomeTeamScore,
      etAwayTeamScore,
      pHomeTeamScore,
      pAwayTeamScore,
      attendance
    ) <> (MatchStats.tupled, MatchStats.unapply)
    val id:              Rep[MatchStatsId]       = column[MatchStatsId]("id", O.AutoInc, O.PrimaryKey)
    val htHomeTeamScore: Rep[Option[MatchScore]] = column[Option[MatchScore]]("half_time_home_team_score")
    val htAwayTeamScore: Rep[Option[MatchScore]] = column[Option[MatchScore]]("half_time_away_team_score")
    val ftHomeTeamScore: Rep[Option[MatchScore]] = column[Option[MatchScore]]("full_time_home_team_score")
    val ftAwayTeamScore: Rep[Option[MatchScore]] = column[Option[MatchScore]]("full_time_away_team_score")
    val etHomeTeamScore: Rep[Option[MatchScore]] = column[Option[MatchScore]]("extra_time_home_team_score")
    val etAwayTeamScore: Rep[Option[MatchScore]] = column[Option[MatchScore]]("extra_time_away_team_score")
    val pHomeTeamScore:  Rep[Option[MatchScore]] = column[Option[MatchScore]]("penalty_home_team_score")
    val pAwayTeamScore:  Rep[Option[MatchScore]] = column[Option[MatchScore]]("penalty_away_team_score")
    val attendance:      Rep[Option[Attendance]] = column[Option[Attendance]]("attendance")
  }

  class PlayerTable(tag: Tag) extends Table[Player](tag, None, "players") {
    override def * =
      (id, firstName, lastName, birthday, age, weight, height, image, countryId) <> (Player.tupled, Player.unapply)
    val id:        Rep[PlayerId]            = column[PlayerId]("id", O.AutoInc, O.PrimaryKey)
    val firstName: Rep[FirstName]           = column[FirstName]("first_name")
    val lastName:  Rep[LastName]            = column[LastName]("last_name")
    val birthday:  Rep[Date]                = column[Date]("birthday")
    val age:       Rep[Age]                 = column[Age]("age")
    val weight:    Rep[Option[Weight]]      = column[Option[Weight]]("weight")
    val height:    Rep[Option[Height]]      = column[Option[Height]]("height")
    val image:     Rep[Option[PlayerImage]] = column[Option[PlayerImage]]("image")
    val countryId: Rep[CountryId]           = column[CountryId]("country_id")
    def country = foreignKey("FK_players_countries", countryId, countryTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class PlayerStatsTable(tag: Tag) extends Table[PlayerStats](tag, None, "player_stats") {
    override def * =
      (
        id,
        shirtNumber,
        position,
        startMinute,
        playedMinutes,
        goals,
        assists,
        successfulTackles,
        totalTackles,
        successfulPasses,
        totalPasses,
        successfulDribbling,
        totalDribbling
      ) <> (PlayerStats.tupled, PlayerStats.unapply)
    val id:                  Rep[PlayerStatsId] = column[PlayerStatsId]("id", O.AutoInc, O.PrimaryKey)
    val shirtNumber:         Rep[ShirtNumber]   = column[ShirtNumber]("t-shirt_number")
    val position:            Rep[Position]      = column[Position]("position")
    val startMinute:         Rep[Minute]        = column[Minute]("start_minute")
    val playedMinutes:       Rep[Minute]        = column[Minute]("played_minutes")
    val goals:               Rep[Goals]         = column[Goals]("goals")
    val assists:             Rep[Assists]       = column[Assists]("assists")
    val successfulTackles:   Rep[Tackles]       = column[Tackles]("successful_tackles")
    val totalTackles:        Rep[Tackles]       = column[Tackles]("total_tackles")
    val successfulPasses:    Rep[Passes]        = column[Passes]("successful_passes")
    val totalPasses:         Rep[Passes]        = column[Passes]("total_passes")
    val successfulDribbling: Rep[Dribbling]     = column[Dribbling]("successful_dribblings")
    val totalDribbling:      Rep[Dribbling]     = column[Dribbling]("total_dribblings")
  }

  class RefereeTable(tag: Tag) extends Table[Referee](tag, None, "referees") {
    override def * = (id, firstName, lastName, image, countryId) <> (Referee.tupled, Referee.unapply)
    val id:        Rep[RefereeId]            = column[RefereeId]("id", O.AutoInc, O.PrimaryKey)
    val firstName: Rep[RefereeFirstName]     = column[RefereeFirstName]("first_name")
    val lastName:  Rep[RefereeLastName]      = column[RefereeLastName]("last_name")
    val image:     Rep[Option[RefereeImage]] = column[Option[RefereeImage]]("image")
    val countryId: Rep[CountryId]            = column[CountryId]("country_id")
    def country = foreignKey("FK_referees_countries", countryId, countryTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class SeasonTable(tag: Tag) extends Table[Season](tag, None, "seasons") {
    override def * = (id, name, isCurrent, startDate, endDate) <> (Season.tupled, Season.unapply)
    val id:        Rep[SeasonId]   = column[SeasonId]("id", O.AutoInc, O.PrimaryKey)
    val name:      Rep[SeasonName] = column[SeasonName]("name", O.Unique)
    val isCurrent: Rep[Boolean]    = column[Boolean]("is_current")
    val startDate: Rep[Date]       = column[Date]("start_date")
    val endDate:   Rep[Date]       = column[Date]("end_date")
  }

  class StageTable(tag: Tag) extends Table[Stage](tag, None, "stages") {
    override def * = (id, name) <> (Stage.tupled, Stage.unapply)
    val id:   Rep[StageId]   = column[StageId]("id", O.AutoInc, O.PrimaryKey)
    val name: Rep[StageName] = column[StageName]("name", O.Unique)
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

  class UserTable(tag: Tag) extends Table[User](tag, None, "users") {
    override def * = (id, login, passwordHash, email, role) <> (User.tupled, User.unapply)
    val id:           Rep[UserId]       = column[UserId]("id", O.AutoInc, O.PrimaryKey)
    val login:        Rep[Login]        = column[Login]("login", O.Unique)
    val passwordHash: Rep[PasswordHash] = column[PasswordHash]("password_hash")
    val email:        Rep[Email]        = column[Email]("email")
    val role:         Rep[Role]         = column[Role]("role")
  }

  class UserSubscriptionsOnPlayerTable(tag: Tag)
    extends Table[UserSubscriptionOnPlayer](tag, None, "user_subscriptions_on_players") {
    override def * = (userId, playerId) <> (UserSubscriptionOnPlayer.tupled, UserSubscriptionOnPlayer.unapply)
    val userId:   Rep[UserId]   = column[UserId]("user_id")
    val playerId: Rep[PlayerId] = column[PlayerId]("player_id")

    def pk = primaryKey("PK_user_subscriptions_on_players", (userId, playerId))
    def user = foreignKey("FK_user_subscriptions_on_players_users", userId, userTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def player = foreignKey("FK_user_subscriptions_on_players_players", playerId, playerTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class UserSubscriptionsOnTeamTable(tag: Tag)
    extends Table[UserSubscriptionOnTeam](tag, None, "user_subscriptions_on_teams") {
    override def * = (userId, teamId) <> (UserSubscriptionOnTeam.tupled, UserSubscriptionOnTeam.unapply)
    val userId: Rep[UserId] = column[UserId]("user_id")
    val teamId: Rep[TeamId] = column[TeamId]("team_id")

    def pk = primaryKey("PK_user_subscriptions_on_teams", (userId, teamId))
    def user = foreignKey("FK_user_subscriptions_on_teams_users", userId, userTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def team = foreignKey("FK_user_subscriptions_on_teams_teams", teamId, teamTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class VenueTable(tag: Tag) extends Table[Venue](tag, None, "venues") {
    override def * = (id, name, capacity, city, countryId) <> (Venue.tupled, Venue.unapply)
    val id:        Rep[VenueId]   = column[VenueId]("id", O.AutoInc, O.PrimaryKey)
    val name:      Rep[VenueName] = column[VenueName]("name", O.Unique)
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

  val matchStatsTable = TableQuery[MatchStatsTable]

  val playerTable = TableQuery[PlayerTable]

  val playerStatsTable = TableQuery[PlayerStatsTable]

  val refereeTable = TableQuery[RefereeTable]

  val seasonTable = TableQuery[SeasonTable]

  val stageTable = TableQuery[StageTable]

  val teamTable = TableQuery[TeamTable]

  val userTable = TableQuery[UserTable]

  val userPlayersTable = TableQuery[UserSubscriptionsOnPlayerTable]

  val userTeamsTable = TableQuery[UserSubscriptionsOnTeamTable]

  val venueTable = TableQuery[VenueTable]
}
