package com.itechart.project.dto

import com.itechart.project.dto.country_dto.CountryApiDto
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonConverters {

  trait CountryJsonProtocol extends DefaultJsonProtocol {
    implicit val countryFormat: RootJsonFormat[CountryApiDto] = jsonFormat4(CountryApiDto)
  }

}
