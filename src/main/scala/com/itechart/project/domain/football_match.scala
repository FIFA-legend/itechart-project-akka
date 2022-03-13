package com.itechart.project.domain

import com.itechart.project.domain.formation.FormationId
import com.itechart.project.domain.league.LeagueId
import com.itechart.project.domain.match_stats.MatchStatsId
import com.itechart.project.domain.referee.RefereeId
import com.itechart.project.domain.season.SeasonId
import com.itechart.project.domain.stage.StageId
import com.itechart.project.domain.team.TeamId
import com.itechart.project.domain.venue.VenueId
import eu.timepit.refined.types.string.NonEmptyString

import java.sql.{Date, Time}

object football_match {

  final case class MatchId(value: Long)

  type Status = NonEmptyString

  final case class Match(
    id:                  MatchId,
    seasonId:            SeasonId,
    leagueId:            LeagueId,
    stageId:             StageId,
    status:              Status,
    startDate:           Date,
    startTime:           Time,
    homeTeamId:          TeamId,
    awayTeamId:          TeamId,
    venueId:             VenueId,
    refereeId:           RefereeId,
    matchStatsId:        MatchStatsId,
    homeTeamFormationId: FormationId,
    awayTeamFormationId: FormationId
  )

}
