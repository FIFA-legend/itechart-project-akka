package com.itechart.project.dto

import com.itechart.project.dto.country.CountryApiDto
import com.itechart.project.dto.formation.FormationApiDto
import com.itechart.project.dto.league.LeagueApiDto
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

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

}
