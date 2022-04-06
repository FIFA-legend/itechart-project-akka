package com.itechart.project.service.domain_errors

object TeamErrors {

  sealed trait TeamError {
    def message: String
  }

  object TeamError {
    final case class InvalidTeamFullName(name: String) extends TeamError {
      override def message: String =
        s"Invalid team full name `$name`. Team full name must not be empty"
    }

    final case class InvalidTeamShortName(name: String) extends TeamError {
      override def message: String =
        s"Invalid team short name `$name`. Team short name must match regular expression: [A-Z]{3}"
    }

    final case class InvalidTeamLogo(logo: String) extends TeamError {
      override def message: String =
        s"Invalid team logo `$logo`. Team logo must match regular expression: [0-9]+.(png|jpg|jpeg)"
    }

    final case class InvalidTeamCountryId(id: Int) extends TeamError {
      override def message: String =
        s"Invalid team country id `$id`. Country with id `$id` doesn't exist"
    }

    final case class TeamForeignKey(id: Int) extends TeamError {
      override def message: String = s"Team with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
