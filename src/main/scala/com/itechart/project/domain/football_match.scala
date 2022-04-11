package com.itechart.project.domain

import com.itechart.project.domain.formation.FormationId
import com.itechart.project.domain.league.LeagueId
import com.itechart.project.domain.match_stats.MatchStatsId
import com.itechart.project.domain.referee.RefereeId
import com.itechart.project.domain.season.SeasonId
import com.itechart.project.domain.stage.StageId
import com.itechart.project.domain.team.TeamId
import com.itechart.project.domain.venue.VenueId
import enumeratum.{Enum, EnumEntry}

import java.time.{LocalDate, LocalTime}

object football_match {

  final case class MatchId(value: Long)

  sealed trait Status extends EnumEntry

  object Status extends Enum[Status] {
    final case object NotStarted extends Status
    final case object InPlay extends Status
    final case object UpdateLater extends Status
    final case object Ended extends Status
    final case object Postponed extends Status
    final case object Cancelled extends Status
    final case object Abandoned extends Status
    final case object Interrupted extends Status
    final case object Suspended extends Status
    final case object Awarded extends Status
    final case object Delayed extends Status
    final case object HalfTime extends Status
    final case object ExtraTime extends Status
    final case object Penalties extends Status
    final case object BreakTime extends Status
    final case object Awarding extends Status
    final case object ToBeAnnounced extends Status
    final case object AfterPenalties extends Status
    final case object AfterExtraTime extends Status

    override def values: IndexedSeq[Status] = findValues
  }

  final case class Match(
    id:                  MatchId,
    seasonId:            SeasonId,
    leagueId:            LeagueId,
    stageId:             StageId,
    status:              Status,
    startDate:           LocalDate,
    startTime:           LocalTime,
    homeTeamId:          TeamId,
    awayTeamId:          TeamId,
    venueId:             VenueId,
    refereeId:           RefereeId,
    matchStatsId:        MatchStatsId,
    homeTeamFormationId: FormationId,
    awayTeamFormationId: FormationId
  )

}
