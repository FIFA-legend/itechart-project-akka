package com.itechart.project.dto

object league_dto {

  final case class LeagueApiDto(
    league_id:  Int,
    country_id: Int,
    name:       String
  )

}
