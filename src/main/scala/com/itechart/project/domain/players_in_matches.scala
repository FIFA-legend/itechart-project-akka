package com.itechart.project.domain

import com.itechart.project.domain.football_match.MatchId
import com.itechart.project.domain.player.PlayerId
import com.itechart.project.domain.player_stats.PlayerStatsId

object players_in_matches {

  final case class PlayerInMatch(
    matchId:          MatchId,
    playerId:         PlayerId,
    isHomeTeamPlayer: Boolean,
    playerStatsId:    PlayerStatsId
  )

}
