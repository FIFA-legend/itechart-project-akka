package com.itechart.project.dto

object user {

  final case class UserApiDto(
    id:    Long,
    login: String,
    email: String,
    role:  String
  )

  final case class CreateUserApiDto(
    login:    String,
    password: String,
    email:    String
  )

  final case class UpdateUserApiDto(
    id:       Long,
    password: String,
    email:    String,
    role:     String
  )

}
