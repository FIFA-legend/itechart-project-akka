package com.itechart.project.dto

object league {

  final case class LeagueApiDto(
    league_id:  Int,
    country_id: Int,
    name:       String
  )

}
