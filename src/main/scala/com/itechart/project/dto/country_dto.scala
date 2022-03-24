package com.itechart.project.dto

object country_dto {

  final case class CountryApiDto(
    id:           Int,
    name:         String,
    country_code: String,
    continent:    String
  )

}
