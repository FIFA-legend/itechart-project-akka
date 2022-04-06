package com.itechart.project.dto

import com.itechart.project.dto.country.CountryApiDto
import com.itechart.project.dto.formation.FormationApiDto
import com.itechart.project.dto.league.LeagueApiDto
import com.itechart.project.dto.season.SeasonApiDto
import com.itechart.project.dto.stage.StageApiDto
import com.itechart.project.dto.team.TeamApiDto
import spray.json._

import java.time.LocalDate

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
          case Seq(JsNumber(id), JsString(name), JsString(shortCode), JsString(logo), JsNumber(countryId)) =>
            TeamApiDto(id.toInt, name, shortCode, Option(logo), countryId.toInt)
          case Seq(JsNumber(id), JsString(name), JsString(shortCode), JsNull, JsNumber(countryId)) =>
            TeamApiDto(id.toInt, name, shortCode, None, countryId.toInt)
          case _ => throw DeserializationException("Team expected")
        }
      }

      override def write(team: TeamApiDto): JsValue = JsObject(
        "team_id"    -> JsNumber(team.id),
        "name"       -> JsString(team.name),
        "short_code" -> JsString(team.shortCode),
        "logo"       -> logoToJsValue(team.logo),
        "country_id" -> JsNumber(team.countryId)
      )

      private def logoToJsValue(logo: Option[String]): JsValue = logo match {
        case None        => JsNull
        case Some(value) => JsString(value)
      }
    }
  }

}
