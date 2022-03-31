package com.itechart.project.dto

object country {

  final case class CountryApiDto(
    id:          Int,
    name:        String,
    countryCode: String,
    continent:   String
  )

}
