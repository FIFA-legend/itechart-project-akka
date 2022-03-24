package com.itechart.project.service.domain_errors

object LeagueErrors {

  sealed trait LeagueError {
    def message: String
  }

  object LeagueError {
    final case class InvalidLeagueName(name: String) extends LeagueError {
      override def message: String =
        s"Invalid league name `$name`. League name must not be empty"
    }

    final case class DuplicateLeagueName(name: String) extends LeagueError {
      override def message: String = s"Duplicate league name `$name`. League name must be unique"
    }

    final case class InvalidLeagueCountryId(id: Int) extends LeagueError {
      override def message: String =
        s"Invalid league country id `$id`. Country with id `$id` doesn't exist"
    }

    final case class LeagueNotDeleted(id: Int) extends LeagueError {
      override def message: String = s"League with id `$id` can't be deleted because it's a part of foreign key"
    }

    final case object LeagueOperationFail extends LeagueError {
      override def message: String = s"Some internal server exception happened during request"
    }
  }

}
