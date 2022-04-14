package com.itechart.project.dto

import com.itechart.project.dto.country.CountryApiDto
import com.itechart.project.dto.football_match.MatchApiDto
import com.itechart.project.dto.formation.FormationApiDto
import com.itechart.project.dto.league.LeagueApiDto
import com.itechart.project.dto.match_stats.MatchStatsApiDto
import com.itechart.project.dto.player.PlayerApiDto
import com.itechart.project.dto.referee.RefereeApiDto
import com.itechart.project.dto.season.SeasonApiDto
import com.itechart.project.dto.stage.StageApiDto
import com.itechart.project.dto.team.TeamApiDto
import com.itechart.project.dto.user.{CreateUserApiDto, UpdateUserApiDto, UserApiDto}
import com.itechart.project.dto.venue.VenueApiDto
import spray.json._

import java.time.{LocalDate, LocalTime}

object JsonConverters {

  trait CountryJsonProtocol extends DefaultJsonProtocol {
    implicit object CountryJsonFormat extends RootJsonFormat[CountryApiDto] {
      override def read(value: JsValue): CountryApiDto = {
        value.asJsObject.getFields("id", "name", "country_code", "continent") match {
          case Seq(JsNumber(id), JsString(name), JsString(countryCode), JsString(continent)) =>
            CountryApiDto(id.toInt, name, countryCode, continent)
          case _ => throw DeserializationException("Country expected")
        }
      }

      override def write(country: CountryApiDto): JsValue = JsObject(
        "id"           -> JsNumber(country.id),
        "name"         -> JsString(country.name),
        "country_code" -> JsString(country.countryCode),
        "continent"    -> JsString(country.continent)
      )
    }
  }

  trait FormationJsonProtocol extends DefaultJsonProtocol {
    implicit object FormationJsonFormat extends RootJsonFormat[FormationApiDto] {
      override def read(value: JsValue): FormationApiDto = {
        value.asJsObject.getFields("formation_id", "name") match {
          case Seq(JsNumber(id), JsString(name)) =>
            FormationApiDto(id.toInt, name)
          case _ => throw DeserializationException("Formation expected")
        }
      }

      override def write(formation: FormationApiDto): JsValue = JsObject(
        "formation_id" -> JsNumber(formation.id),
        "name"         -> JsString(formation.name)
      )
    }
  }

  trait LeagueJsonProtocol extends DefaultJsonProtocol {
    implicit object LeagueJsonFormat extends RootJsonFormat[LeagueApiDto] {
      override def read(value: JsValue): LeagueApiDto = {
        value.asJsObject.getFields("league_id", "country_id", "name") match {
          case Seq(JsNumber(id), JsNumber(countryId), JsString(name)) =>
            LeagueApiDto(id.toInt, countryId.toInt, name)
          case _ => throw DeserializationException("League expected")
        }
      }

      override def write(league: LeagueApiDto): JsValue = JsObject(
        "league_id"  -> JsNumber(league.id),
        "country_id" -> JsNumber(league.countryId),
        "name"       -> JsString(league.name)
      )
    }
  }

  trait MatchJsonProtocol extends DefaultJsonProtocol {
    implicit object MatchJsonFormat extends RootJsonFormat[MatchApiDto] {
      override def read(value: JsValue): MatchApiDto = {
        value.asJsObject.getFields(
          "match_id",
          "season_id",
          "league_id",
          "stage_id",
          "status",
          "start_date",
          "start_time",
          "home_team_id",
          "away_team_id",
          "venue_id",
          "referee_id",
          "match_stats_id",
          "home_team_formation_id",
          "away_team_formation_id"
        ) match {
          case Seq(
                JsNumber(id),
                JsNumber(seasonId),
                JsNumber(leagueId),
                JsNumber(stageId),
                JsString(status),
                JsString(startDate),
                JsString(startTime),
                JsNumber(homeTeamId),
                JsNumber(awayTeamId),
                JsNumber(venueId),
                JsNumber(refereeId),
                JsNumber(matchStatsId),
                JsNumber(homeTeamFormationId),
                JsNumber(awayTeamFormationId)
              ) =>
            MatchApiDto(
              id.toLong,
              seasonId.toInt,
              leagueId.toInt,
              stageId.toInt,
              status,
              LocalDate.parse(startDate),
              LocalTime.parse(startTime),
              homeTeamId.toInt,
              awayTeamId.toInt,
              venueId.toInt,
              refereeId.toInt,
              matchStatsId.toLong,
              homeTeamFormationId.toInt,
              awayTeamFormationId.toInt
            )
          case _ => throw DeserializationException("League expected")
        }
      }

      override def write(footballMatch: MatchApiDto): JsValue = JsObject(
        "match_id"               -> JsNumber(footballMatch.id),
        "season_id"              -> JsNumber(footballMatch.seasonId),
        "league_id"              -> JsNumber(footballMatch.leagueId),
        "stage_id"               -> JsNumber(footballMatch.stageId),
        "status"                 -> JsString(footballMatch.status),
        "start_date"             -> JsString(footballMatch.startDate.toString),
        "start_time"             -> JsString(footballMatch.startTime.toString),
        "home_team_id"           -> JsNumber(footballMatch.homeTeamId),
        "away_team_id"           -> JsNumber(footballMatch.awayTeamId),
        "venue_id"               -> JsNumber(footballMatch.venueId),
        "referee_id"             -> JsNumber(footballMatch.refereeId),
        "match_stats_id"         -> JsNumber(footballMatch.matchStatsId),
        "home_team_formation_id" -> JsNumber(footballMatch.homeTeamFormationId),
        "away_team_formation_id" -> JsNumber(footballMatch.awayTeamFormationId)
      )
    }
  }

  trait MatchStatsJsonProtocol extends DefaultJsonProtocol {
    implicit object MatchStatsJsonFormat extends RootJsonFormat[MatchStatsApiDto] {
      override def read(value: JsValue): MatchStatsApiDto = {
        value.asJsObject.getFields(
          "match_stats_id",
          "ht_home_team_score",
          "ht_away_team_score",
          "ft_home_team_score",
          "ft_away_team_score",
          "et_home_team_score",
          "et_away_team_score",
          "p_home_team_score",
          "p_away_team_score",
          "attendance"
        ) match {
          case Seq(
                JsNumber(id),
                htHome:     JsValue,
                htAway:     JsValue,
                ftHome:     JsValue,
                ftAway:     JsValue,
                etHome:     JsValue,
                etAway:     JsValue,
                pHome:      JsValue,
                pAway:      JsValue,
                attendance: JsValue
              ) =>
            MatchStatsApiDto(
              id.toLong,
              jsValueToOptionInt(htHome),
              jsValueToOptionInt(htAway),
              jsValueToOptionInt(ftHome),
              jsValueToOptionInt(ftAway),
              jsValueToOptionInt(etHome),
              jsValueToOptionInt(etAway),
              jsValueToOptionInt(pHome),
              jsValueToOptionInt(pAway),
              jsValueToOptionInt(attendance)
            )
          case _ => throw DeserializationException("Match stats expected")
        }
      }

      override def write(matchStats: MatchStatsApiDto): JsValue = JsObject(
        "match_stats_id"     -> JsNumber(matchStats.id),
        "ht_home_team_score" -> optionIntToJsValue(matchStats.htHomeTeamScore),
        "ht_away_team_score" -> optionIntToJsValue(matchStats.htAwayTeamScore),
        "ft_home_team_score" -> optionIntToJsValue(matchStats.ftHomeTeamScore),
        "ft_away_team_score" -> optionIntToJsValue(matchStats.ftAwayTeamScore),
        "et_home_team_score" -> optionIntToJsValue(matchStats.etHomeTeamScore),
        "et_away_team_score" -> optionIntToJsValue(matchStats.etAwayTeamScore),
        "p_home_team_score"  -> optionIntToJsValue(matchStats.pHomeTeamScore),
        "p_away_team_score"  -> optionIntToJsValue(matchStats.pAwayTeamScore),
        "attendance"         -> optionIntToJsValue(matchStats.attendance)
      )
    }
  }

  trait PlayerJsonProtocol extends DefaultJsonProtocol {
    implicit object PlayerJsonFormat extends RootJsonFormat[PlayerApiDto] {
      override def read(value: JsValue): PlayerApiDto = {
        value.asJsObject.getFields(
          "player_id",
          "first_name",
          "last_name",
          "birthday",
          "age",
          "weight",
          "height",
          "img",
          "country_id"
        ) match {
          case Seq(
                JsNumber(id),
                JsString(firstName),
                JsString(lastName),
                JsString(birthday),
                JsNumber(age),
                weight: JsValue,
                height: JsValue,
                image:  JsValue,
                JsNumber(countryId)
              ) =>
            PlayerApiDto(
              id.toInt,
              firstName,
              lastName,
              LocalDate.parse(birthday),
              age.toInt,
              jsValueToOptionInt(weight),
              jsValueToOptionInt(height),
              jsValueToOptionString(image),
              countryId.toInt
            )
          case _ => throw DeserializationException("Player expected")
        }
      }

      override def write(player: PlayerApiDto): JsValue = JsObject(
        "player_id"  -> JsNumber(player.id),
        "first_name" -> JsString(player.firstName),
        "last_name"  -> JsString(player.lastName),
        "birthday"   -> JsString(player.birthday.toString),
        "age"        -> JsNumber(player.age),
        "weight"     -> optionIntToJsValue(player.weight),
        "height"     -> optionIntToJsValue(player.height),
        "img"        -> optionStringToJsValue(player.image),
        "country_id" -> JsNumber(player.countryId)
      )
    }
  }

  trait RefereeJsonProtocol extends DefaultJsonProtocol {
    implicit object RefereeJsonFormat extends RootJsonFormat[RefereeApiDto] {
      override def read(value: JsValue): RefereeApiDto = {
        value.asJsObject.getFields("referee_id", "first_name", "last_name", "img", "country_id") match {
          case Seq(JsNumber(id), JsString(firstName), JsString(lastName), image: JsValue, JsNumber(countryId)) =>
            RefereeApiDto(id.toInt, firstName, lastName, jsValueToOptionString(image), countryId.toInt)
          case _ => throw DeserializationException("Referee expected")
        }
      }

      override def write(referee: RefereeApiDto): JsValue = JsObject(
        "referee_id" -> JsNumber(referee.id),
        "first_name" -> JsString(referee.firstName),
        "last_name"  -> JsString(referee.lastName),
        "img"        -> optionStringToJsValue(referee.image),
        "country_id" -> JsNumber(referee.countryId)
      )
    }
  }

  trait SeasonJsonProtocol extends DefaultJsonProtocol {
    implicit object SeasonJsonFormat extends RootJsonFormat[SeasonApiDto] {
      override def read(value: JsValue): SeasonApiDto = {
        value.asJsObject.getFields("season_id", "name", "is_current", "start_date", "end_date") match {
          case Seq(JsNumber(id), JsString(name), JsBoolean(isCurrent), JsString(startDate), JsString(endDate)) =>
            SeasonApiDto(id.toInt, name, isCurrent, LocalDate.parse(startDate), LocalDate.parse(endDate))
          case _ => throw DeserializationException("Season expected")
        }
      }

      override def write(season: SeasonApiDto): JsValue = JsObject(
        "season_id"  -> JsNumber(season.id),
        "name"       -> JsString(season.name),
        "is_current" -> JsBoolean(season.isCurrent),
        "start_date" -> JsString(season.startDate.toString),
        "end_date"   -> JsString(season.endDate.toString)
      )
    }
  }

  trait StageJsonProtocol extends DefaultJsonProtocol {
    implicit object StageJsonFormat extends RootJsonFormat[StageApiDto] {
      override def read(value: JsValue): StageApiDto = {
        value.asJsObject.getFields("stage_id", "name") match {
          case Seq(JsNumber(id), JsString(name)) =>
            StageApiDto(id.toInt, name)
          case _ => throw DeserializationException("Stage expected")
        }
      }

      override def write(stage: StageApiDto): JsValue = JsObject(
        "stage_id" -> JsNumber(stage.id),
        "name"     -> JsString(stage.name)
      )
    }
  }

  trait TeamJsonProtocol extends DefaultJsonProtocol {
    implicit object TeamJsonFormat extends RootJsonFormat[TeamApiDto] {
      override def read(value: JsValue): TeamApiDto = {
        value.asJsObject.getFields("team_id", "name", "short_code", "logo", "country_id") match {
          case Seq(JsNumber(id), JsString(name), JsString(shortCode), logo: JsValue, JsNumber(countryId)) =>
            TeamApiDto(id.toInt, name, shortCode, jsValueToOptionString(logo), countryId.toInt)
          case _ => throw DeserializationException("Team expected")
        }
      }

      override def write(team: TeamApiDto): JsValue = JsObject(
        "team_id"    -> JsNumber(team.id),
        "name"       -> JsString(team.name),
        "short_code" -> JsString(team.shortCode),
        "logo"       -> optionStringToJsValue(team.logo),
        "country_id" -> JsNumber(team.countryId)
      )
    }
  }

  trait UserJsonProtocol extends DefaultJsonProtocol {
    implicit object UserJsonFormat extends RootJsonFormat[UserApiDto] {
      override def write(user: UserApiDto): JsValue = JsObject(
        "user_id" -> JsNumber(user.id),
        "login"   -> JsString(user.login),
        "email"   -> JsString(user.email),
        "role"    -> JsString(user.role)
      )

      override def read(value: JsValue): UserApiDto = {
        value.asJsObject.getFields("user_id", "login", "email", "role") match {
          case Seq(JsNumber(id), JsString(login), JsString(email), JsString(role)) =>
            UserApiDto(id.toLong, login, email, role)
          case _ => throw DeserializationException("User expected")
        }
      }
    }

    implicit object CreateUserJsonFormat extends RootJsonReader[CreateUserApiDto] {
      override def read(value: JsValue): CreateUserApiDto = {
        value.asJsObject.getFields("login", "password", "email") match {
          case Seq(JsString(login), JsString(password), JsString(email)) =>
            CreateUserApiDto(login, password, email)
          case _ => throw DeserializationException("User expected")
        }
      }
    }

    implicit object UpdateUserJsonFormat extends RootJsonReader[UpdateUserApiDto] {
      override def read(value: JsValue): UpdateUserApiDto = {
        value.asJsObject.getFields("user_id", "password", "email", "role") match {
          case Seq(JsNumber(id), JsString(password), JsString(email), JsString(role)) =>
            UpdateUserApiDto(id.toLong, password, email, role)
          case _ => throw DeserializationException("User expected")
        }
      }
    }
  }

  trait VenueJsonProtocol extends DefaultJsonProtocol {
    implicit object VenueJsonFormat extends RootJsonFormat[VenueApiDto] {
      override def read(value: JsValue): VenueApiDto = {
        value.asJsObject.getFields("venue_id", "name", "capacity", "city", "country_id") match {
          case Seq(JsNumber(id), JsString(name), JsNumber(capacity), JsString(city), JsNumber(countryId)) =>
            VenueApiDto(id.toInt, name, capacity.toInt, city, countryId.toInt)
          case _ => throw DeserializationException("Venue expected")
        }
      }

      override def write(venue: VenueApiDto): JsValue = JsObject(
        "venue_id"   -> JsNumber(venue.id),
        "name"       -> JsString(venue.name),
        "capacity"   -> JsNumber(venue.capacity),
        "city"       -> JsString(venue.city),
        "country_id" -> JsNumber(venue.countryId)
      )
    }
  }

  private def jsValueToOptionString(jsValue: JsValue): Option[String] = jsValue match {
    case JsString(value) => Option(value)
    case _               => None
  }

  private def optionStringToJsValue(option: Option[String]): JsValue = option match {
    case None         => JsNull
    case Some(string) => JsString(string)
  }

  private def jsValueToOptionInt(jsValue: JsValue): Option[Int] = jsValue match {
    case JsNumber(value) => Option(value.toInt)
    case _               => None
  }

  private def optionIntToJsValue(option: Option[Int]): JsValue = option match {
    case None      => JsNull
    case Some(int) => JsNumber(int)
  }

}
