package com.itechart.project.domain

import akka.http.scaladsl.model.DateTime
import com.itechart.project.domain.formation.FormationId
import com.itechart.project.domain.league.LeagueId
import com.itechart.project.domain.referee.RefereeId
import com.itechart.project.domain.season.SeasonId
import com.itechart.project.domain.stats.StatsId
import com.itechart.project.domain.team.TeamId
import com.itechart.project.domain.venue.VenueId
import eu.timepit.refined.types.string.NonEmptyString

object football_match {

  final case class MatchId(value: Long)

  type Status = NonEmptyString

  final case class Match(
    id:                  MatchId,
    leagueId:            LeagueId,
    seasonId:            SeasonId,
    statusCode:          Int,
    status:              Status,
    matchStart:          DateTime,
    homeTeamId:          TeamId,
    awayTeamId:          TeamId,
    venueId:             VenueId,
    refereeId:           RefereeId,
    statsId:             StatsId,
    homeTeamFormationId: FormationId,
    awayTeamFormationId: FormationId
  )

}
