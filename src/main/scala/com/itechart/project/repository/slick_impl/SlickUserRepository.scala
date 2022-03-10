package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.user.{Login, User, UserId}
import com.itechart.project.repository.UserRepository
import com.itechart.project.repository.slick_impl.Implicits._
import com.itechart.project.repository.slick_impl.Tables._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SlickUserRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext) extends UserRepository {

  override def findAll: Future[List[User]] = {
    val allUsersQuery = userTable.result
    db.run[Seq[User]](allUsersQuery).map(_.toList)
  }

  override def findById(id: UserId): Future[Option[User]] = {
    val userByIdQuery = userTable.filter(_.id === id)
    db.run[Seq[User]](userByIdQuery.result).map(_.headOption)
  }

  override def findByLogin(login: Login): Future[Option[User]] = {
    val userByLoginQuery = userTable.filter(_.login === login)
    db.run[Seq[User]](userByLoginQuery.result).map(_.headOption)
  }

  override def create(user: User): Future[UserId] = {
    val insertUserQuery = (userTable returning userTable.map(_.id)) += user
    db.run(insertUserQuery)
  }

  override def update(user: User): Future[Int] = {
    val updateUserQuery = userTable
      .filter(_.id === user.id)
      .map(user => (user.login, user.passwordHash, user.email, user.role))
      .update((user.login, user.passwordHash, user.email, user.role))
    db.run(updateUserQuery)
  }

  override def delete(id: UserId): Future[Int] = {
    val deleteUserQuery = userTable.filter(_.id === id).delete
    db.run(deleteUserQuery)
  }
}
