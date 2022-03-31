package com.itechart.project.dto

object league {

  final case class LeagueApiDto(
    id:        Int,
    countryId: Int,
    name:      String
  )

}
