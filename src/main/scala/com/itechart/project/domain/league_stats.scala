package com.itechart.project.domain

import com.itechart.project.domain.league.LeagueId
import com.itechart.project.domain.season.SeasonId
import com.itechart.project.domain.team.TeamId
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative

object league_stats {

  type Place = Int Refined NonNegative

  type LeagueGoals = Int Refined NonNegative

  type LeagueMatches = Int Refined NonNegative

  final case class LeagueStats(
    seasonId:      SeasonId,
    leagueId:      LeagueId,
    teamId:        TeamId,
    place:         Place,
    points:        Int,
    scoredGoals:   LeagueGoals,
    concededGoals: LeagueGoals,
    victories:     LeagueMatches,
    defeats:       LeagueMatches,
    draws:         LeagueMatches
  )

}
