package com.itechart.project.dto

object country_dto {

  case class CountryApiDto(
    id:           Int,
    name:         String,
    country_code: String,
    continent:    String
  )

}
