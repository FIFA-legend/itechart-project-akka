package com.itechart.project.domain

import enumeratum.{Enum, EnumEntry}
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

object user {

  final case class UserId(value: Long)

  final case class Login(value: String)

  final case class PasswordHash(value: String)

  type Email = String Refined MatchesRegex[W.`"^[A-Za-z0-9_]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]

  sealed trait Role extends EnumEntry

  object Role extends Enum[Role] {
    final case object Admin extends Role
    final case object User extends Role

    override def values: IndexedSeq[Role] = findValues
  }

  final case class User(
    id:           UserId,
    login:        Login,
    passwordHash: PasswordHash,
    email:        Email,
    role:         Role
  )

}
