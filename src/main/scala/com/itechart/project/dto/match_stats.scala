package com.itechart.project.dto

object match_stats {

  final case class MatchStatsApiDto(
    id:              Long,
    htHomeTeamScore: Option[Int],
    htAwayTeamScore: Option[Int],
    ftHomeTeamScore: Option[Int],
    ftAwayTeamScore: Option[Int],
    etHomeTeamScore: Option[Int],
    etAwayTeamScore: Option[Int],
    pHomeTeamScore:  Option[Int],
    pAwayTeamScore:  Option[Int],
    attendance:      Option[Int]
  )

}
