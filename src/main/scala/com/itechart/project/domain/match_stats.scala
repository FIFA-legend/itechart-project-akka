package com.itechart.project.domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative

object match_stats {

  final case class MatchStatsId(value: Long)

  type MatchScore = Int Refined NonNegative

  type Attendance = Int Refined NonNegative

  final case class MatchStats(
    id:              MatchStatsId,
    htHomeTeamScore: Option[MatchScore],
    htAwayTeamScore: Option[MatchScore],
    ftHomeTeamScore: Option[MatchScore],
    ftAwayTeamScore: Option[MatchScore],
    etHomeTeamScore: Option[MatchScore],
    etAwayTeamScore: Option[MatchScore],
    pHomeTeamScore:  Option[MatchScore],
    pAwayTeamScore:  Option[MatchScore],
    attendance:      Option[Attendance]
  )

}
