package com.itechart.project.domain

import com.itechart.project.domain.player.PlayerId
import com.itechart.project.domain.team.TeamId
import com.itechart.project.domain.user.UserId

object user_subscriptions {

  final case class UserSubscriptionOnPlayer(
    userId:   UserId,
    playerId: PlayerId
  )

  final case class UserSubscriptionOnTeam(
    userId: UserId,
    teamId: TeamId
  )

}
