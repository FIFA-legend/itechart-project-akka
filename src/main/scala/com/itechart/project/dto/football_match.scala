package com.itechart.project.dto

import java.time.{LocalDate, LocalTime}

object football_match {

  final case class MatchApiDto(
    id:                  Long,
    seasonId:            Int,
    leagueId:            Int,
    stageId:             Int,
    status:              String,
    startDate:           LocalDate,
    startTime:           LocalTime,
    homeTeamId:          Int,
    awayTeamId:          Int,
    venueId:             Int,
    refereeId:           Int,
    matchStatsId:        Long,
    homeTeamFormationId: Int,
    awayTeamFormationId: Int
  )

}
