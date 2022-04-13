package com.itechart.project.service.domain_errors

import java.time.LocalDate

object MatchErrors {

  sealed trait MatchError extends DomainError

  object MatchError {
    final case class InvalidMatchStatus(status: String) extends MatchError {
      override def message: String =
        s"Invalid match status `$status`. Such status doesn't exist"
    }

    final case class InvalidMatchStartDate(date: LocalDate) extends MatchError {
      override def message: String =
        s"Invalid match start date `$date`. Start date must be within 6 months"
    }

    final case class InvalidMatchSeasonId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match season id `$id`. Season with id `$id` doesn't exist"
    }

    final case class InvalidMatchLeagueId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match league id `$id`. League with id `$id` doesn't exist"
    }

    final case class InvalidMatchStageId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match stage id `$id`. Stage with id `$id` doesn't exist"
    }

    final case class InvalidMatchHomeTeamId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match home team id `$id`. Team with id `$id` doesn't exist"
    }

    final case class InvalidMatchAwayTeamId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match away team id `$id`. Team with id `$id` doesn't exist"
    }

    final case class InvalidMatchVenueId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match venue id `$id`. Venue with id `$id` doesn't exist"
    }

    final case class InvalidMatchRefereeId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match referee id `$id`. Referee with id `$id` doesn't exist"
    }

    final case class InvalidMatchStatsId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match stats id `$id`. Match stats with id `$id` doesn't exist"
    }

    final case class InvalidMatchHomeTeamFormationId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match home team formation id `$id`. Formation with id `$id` doesn't exist"
    }

    final case class InvalidMatchAwayTeamFormationId(id: Int) extends MatchError {
      override def message: String =
        s"Invalid match away team formation id `$id`. Formation with id `$id` doesn't exist"
    }

    final case class MatchForeignKey(id: Int) extends MatchError {
      override def message: String =
        s"Match with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
