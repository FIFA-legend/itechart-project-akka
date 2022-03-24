package com.itechart.project.dto

import com.itechart.project.dto.country_dto.CountryApiDto
import com.itechart.project.dto.league_dto.LeagueApiDto
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonConverters {

  trait CountryJsonProtocol extends DefaultJsonProtocol {
    implicit val countryFormat: RootJsonFormat[CountryApiDto] = jsonFormat4(CountryApiDto)
  }

  trait LeagueJsonProtocol extends DefaultJsonProtocol {
    implicit val leagueFormat: RootJsonFormat[LeagueApiDto] = jsonFormat3(LeagueApiDto)
  }

}
