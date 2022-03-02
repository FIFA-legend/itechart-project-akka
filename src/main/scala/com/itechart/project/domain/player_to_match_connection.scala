package com.itechart.project.domain

import com.itechart.project.domain.football_match.MatchId
import com.itechart.project.domain.player.PlayerId

object player_to_match_connection {

  final case class PlayerToMatch(
    playerId:   PlayerId,
    matchId:    MatchId,
    isHomeTeam: Boolean
  )

}
