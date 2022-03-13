package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.player.Player
import com.itechart.project.domain.team.Team
import com.itechart.project.domain.user.User
import com.itechart.project.domain.user_subscriptions.{UserSubscriptionOnPlayer, UserSubscriptionOnTeam}
import com.itechart.project.repository.UserSubscriptionsRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickUserSubscriptionsRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends UserSubscriptionsRepository {

  override def findPlayerSubscriptionsByUser(user: User): Future[List[UserSubscriptionOnPlayer]] = {
    val playerSubscriptionsByUserQuery = userPlayersTable.filter(_.userId === user.id)
    db.run[Seq[UserSubscriptionOnPlayer]](playerSubscriptionsByUserQuery.result).map(_.toList)
  }

  override def findPlayerSubscriptionsByPlayer(player: Player): Future[List[UserSubscriptionOnPlayer]] = {
    val playerSubscriptionsByPlayerQuery = userPlayersTable.filter(_.playerId === player.id)
    db.run[Seq[UserSubscriptionOnPlayer]](playerSubscriptionsByPlayerQuery.result).map(_.toList)
  }

  override def createPlayerSubscription(subscription: UserSubscriptionOnPlayer): Future[Int] = {
    val insertUserSubscriptionOnPlayerQuery = userPlayersTable += subscription
    db.run(insertUserSubscriptionOnPlayerQuery)
  }

  override def deletePlayerSubscription(subscription: UserSubscriptionOnPlayer): Future[Int] = {
    val deleteUserSubscriptionOnPlayerQuery = userPlayersTable
      .filter(table => table.userId === subscription.userId && table.playerId === subscription.playerId)
      .delete
    db.run(deleteUserSubscriptionOnPlayerQuery)
  }

  override def findTeamSubscriptionsByUser(user: User): Future[List[UserSubscriptionOnTeam]] = {
    val teamSubscriptionsByUserQuery = userTeamsTable.filter(_.userId === user.id)
    db.run[Seq[UserSubscriptionOnTeam]](teamSubscriptionsByUserQuery.result).map(_.toList)
  }

  override def findTeamSubscriptionsByTeam(team: Team): Future[List[UserSubscriptionOnTeam]] = {
    val teamSubscriptionsByTeamQuery = userTeamsTable.filter(_.teamId === team.id)
    db.run[Seq[UserSubscriptionOnTeam]](teamSubscriptionsByTeamQuery.result).map(_.toList)
  }

  override def createTeamSubscription(subscription: UserSubscriptionOnTeam): Future[Int] = {
    val insertUserSubscriptionOnTeamQuery = userTeamsTable += subscription
    db.run(insertUserSubscriptionOnTeamQuery)
  }

  override def deleteTeamSubscription(subscription: UserSubscriptionOnTeam): Future[Int] = {
    val deleteUserSubscriptionOnTeamQuery = userTeamsTable
      .filter(table => table.userId === subscription.userId && table.teamId === subscription.teamId)
      .delete
    db.run(deleteUserSubscriptionOnTeamQuery)
  }
}
