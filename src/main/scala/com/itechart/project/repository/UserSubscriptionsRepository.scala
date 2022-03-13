package com.itechart.project.repository

import com.itechart.project.domain.player.Player
import com.itechart.project.domain.team.Team
import com.itechart.project.domain.user.User
import com.itechart.project.domain.user_subscriptions.{UserSubscriptionOnPlayer, UserSubscriptionOnTeam}
import com.itechart.project.repository.slick_impl.SlickUserSubscriptionsRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait UserSubscriptionsRepository {
  def findPlayerSubscriptionsByUser(user:     User):                     Future[List[UserSubscriptionOnPlayer]]
  def findPlayerSubscriptionsByPlayer(player: Player):                   Future[List[UserSubscriptionOnPlayer]]
  def createPlayerSubscription(subscription:  UserSubscriptionOnPlayer): Future[Int]
  def deletePlayerSubscription(subscription:  UserSubscriptionOnPlayer): Future[Int]

  def findTeamSubscriptionsByUser(user:    User):                   Future[List[UserSubscriptionOnTeam]]
  def findTeamSubscriptionsByTeam(team:    Team):                   Future[List[UserSubscriptionOnTeam]]
  def createTeamSubscription(subscription: UserSubscriptionOnTeam): Future[Int]
  def deleteTeamSubscription(subscription: UserSubscriptionOnTeam): Future[Int]
}

object UserSubscriptionsRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): UserSubscriptionsRepository =
    new SlickUserSubscriptionsRepository(db)
}
