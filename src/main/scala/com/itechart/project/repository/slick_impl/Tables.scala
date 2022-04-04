package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country._
import com.itechart.project.domain.football_match._
import com.itechart.project.domain.formation._
import com.itechart.project.domain.league._
import com.itechart.project.domain.league_stats._
import com.itechart.project.domain.match_stats._
import com.itechart.project.domain.player._
import com.itechart.project.domain.player_stats._
import com.itechart.project.domain.players_in_matches.PlayerInMatch
import com.itechart.project.domain.referee._
import com.itechart.project.domain.season._
import com.itechart.project.domain.stage._
import com.itechart.project.domain.team._
import com.itechart.project.domain.user._
import com.itechart.project.domain.user_subscriptions._
import com.itechart.project.domain.venue._
import com.itechart.project.repository.slick_impl.Implicits._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import java.time.{LocalDate, LocalTime}

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

  class LeagueStatsTable(tag: Tag) extends Table[LeagueStats](tag, None, "league_stats") {
    override def * = (
      seasonId,
      leagueId,
      teamId,
      place,
      points,
      scoredGoals,
      concededGoals,
      victories,
      defeats,
      draws
    ) <> (LeagueStats.tupled, LeagueStats.unapply)
    val seasonId:      Rep[SeasonId]      = column[SeasonId]("season_id")
    val leagueId:      Rep[LeagueId]      = column[LeagueId]("league_id")
    val teamId:        Rep[TeamId]        = column[TeamId]("team_id")
    val place:         Rep[Place]         = column[Place]("place")
    val points:        Rep[Int]           = column[Int]("points")
    val scoredGoals:   Rep[LeagueGoals]   = column[LeagueGoals]("scored_goals")
    val concededGoals: Rep[LeagueGoals]   = column[LeagueGoals]("conceded_goals")
    val victories:     Rep[LeagueMatches] = column[LeagueMatches]("victories")
    val defeats:       Rep[LeagueMatches] = column[LeagueMatches]("defeats")
    val draws:         Rep[LeagueMatches] = column[LeagueMatches]("draws")

    def pk = primaryKey("PK_league_stats", (seasonId, leagueId, teamId))
    def season = foreignKey("FK_league_stats_seasons", seasonId, seasonTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def league = foreignKey("FK_league_stats_leagues", leagueId, leagueTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def team = foreignKey("FK_league_stats_teams", teamId, teamTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class MatchTable(tag: Tag) extends Table[Match](tag, None, "matches") {
    override def * = (
      id,
      seasonId,
      leagueId,
      stageId,
      status,
      startDate,
      startTime,
      homeTeamId,
      awayTeamId,
      venueId,
      refereeId,
      matchStatsId,
      homeTeamFormationId,
      awayTeamFormationId
    ) <> (Match.tupled, Match.unapply)
    val id:                  Rep[MatchId]      = column[MatchId]("id", O.AutoInc, O.PrimaryKey)
    val seasonId:            Rep[SeasonId]     = column[SeasonId]("season_id")
    val leagueId:            Rep[LeagueId]     = column[LeagueId]("league_id")
    val stageId:             Rep[StageId]      = column[StageId]("stage_id")
    val status:              Rep[Status]       = column[Status]("status")
    val startDate:           Rep[LocalDate]    = column[LocalDate]("start_date")
    val startTime:           Rep[LocalTime]    = column[LocalTime]("start_time")
    val homeTeamId:          Rep[TeamId]       = column[TeamId]("home_team_id")
    val awayTeamId:          Rep[TeamId]       = column[TeamId]("away_team_id")
    val venueId:             Rep[VenueId]      = column[VenueId]("venue_id")
    val refereeId:           Rep[RefereeId]    = column[RefereeId]("referee_id")
    val matchStatsId:        Rep[MatchStatsId] = column[MatchStatsId]("match_stats_id")
    val homeTeamFormationId: Rep[FormationId]  = column[FormationId]("home_team_formation_id")
    val awayTeamFormationId: Rep[FormationId]  = column[FormationId]("away_team_formation_id")

    def season = foreignKey("FK_matches_seasons", seasonId, seasonTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def league = foreignKey("FK_matches_leagues", leagueId, leagueTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def stage = foreignKey("FK_matches_stages", stageId, stageTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def homeTeam = foreignKey("FK_matches_teams", homeTeamId, teamTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def awayTeam = foreignKey("FK_matches_teams_02", awayTeamId, teamTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def venue = foreignKey("FK_matches_venues", venueId, venueTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def referee = foreignKey("FK_matches_referees", refereeId, refereeTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def matchStats = foreignKey("FK_matches_match_stats", matchStatsId, matchStatsTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def homeTeamFormation = foreignKey("FK_matches_formations", homeTeamFormationId, formationTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def awayTeamFormation = foreignKey("FK_matches_formations_02", awayTeamFormationId, formationTable)(
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

  class PlayerInMatchTable(tag: Tag) extends Table[PlayerInMatch](tag, None, "players_in_matches") {
    override def * =
      (matchId, playerId, isHomeTeamPlayer, playerStatsId) <> (PlayerInMatch.tupled, PlayerInMatch.unapply)
    val matchId:          Rep[MatchId]       = column[MatchId]("match_id")
    val playerId:         Rep[PlayerId]      = column[PlayerId]("player_id")
    val isHomeTeamPlayer: Rep[Boolean]       = column[Boolean]("is_home_team_player")
    val playerStatsId:    Rep[PlayerStatsId] = column[PlayerStatsId]("player_stats_id")

    def pk = primaryKey("PK_players_in_matches", (matchId, playerId))
    def matches = foreignKey("FK_players_in_matches_matches", matchId, matchTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def player = foreignKey("FK_players_in_matches_players", playerId, playerTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
    def playerStats = foreignKey("FK_players_in_matches_player_stats", playerStatsId, playerStatsTable)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Restrict
    )
  }

  class PlayerTable(tag: Tag) extends Table[Player](tag, None, "players") {
    override def * =
      (id, firstName, lastName, birthday, age, weight, height, image, countryId) <> (Player.tupled, Player.unapply)
    val id:        Rep[PlayerId]            = column[PlayerId]("id", O.AutoInc, O.PrimaryKey)
    val firstName: Rep[FirstName]           = column[FirstName]("first_name")
    val lastName:  Rep[LastName]            = column[LastName]("last_name")
    val birthday:  Rep[LocalDate]           = column[LocalDate]("birthday")
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
    val startDate: Rep[LocalDate]  = column[LocalDate]("start_date")
    val endDate:   Rep[LocalDate]  = column[LocalDate]("end_date")
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

  val leagueStatsTable = TableQuery[LeagueStatsTable]

  val matchTable = TableQuery[MatchTable]

  val matchStatsTable = TableQuery[MatchStatsTable]

  val playerInMatchTable = TableQuery[PlayerInMatchTable]

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
