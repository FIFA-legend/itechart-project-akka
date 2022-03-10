package com.itechart.project.repository

import com.itechart.project.domain.user.{Login, User, UserId}
import com.itechart.project.repository.slick_impl.SlickUserRepository
import slick.jdbc.MySQLProfile

import scala.concurrent.{ExecutionContext, Future}

trait UserRepository {
  def findAll: Future[List[User]]
  def findById(id:       UserId): Future[Option[User]]
  def findByLogin(login: Login):  Future[Option[User]]
  def create(user:       User):   Future[UserId]
  def update(user:       User):   Future[Int]
  def delete(id:         UserId): Future[Int]
}

object UserRepository {
  def of(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext): UserRepository =
    new SlickUserRepository(db)
}
