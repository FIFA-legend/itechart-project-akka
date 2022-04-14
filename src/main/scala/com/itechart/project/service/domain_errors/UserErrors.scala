package com.itechart.project.service.domain_errors

object UserErrors {

  sealed trait UserError extends DomainError

  object UserError {
    final case class InvalidUserLogin(login: String) extends UserError {
      override def message: String =
        s"Invalid user login `$login`. User login must be 6-32 characters long"
    }

    final case class DuplicateUserLogin(login: String) extends UserError {
      override def message: String =
        s"Duplicate user login `$login`. User login must be unique"
    }

    final case class InvalidUserPassword(password: String) extends UserError {
      override def message: String =
        s"Invalid user password `$password`. User password must be greater than 8 characters long"
    }

    final case class InvalidUserEmail(email: String) extends UserError {
      override def message: String =
        s"Invalid user email `$email`. User email must match [A-Za-z0-9_]+@[A-Za-z0-9]+.[A-Za-z0-9]+"
    }

    final case class InvalidUserRole(role: String) extends UserError {
      override def message: String =
        s"Invalid user role `$role`. User role must be: Admin or User"
    }

    final case class UserForeignKey(id: Long) extends UserError {
      override def message: String =
        s"User with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
