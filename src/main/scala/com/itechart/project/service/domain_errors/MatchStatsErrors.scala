package com.itechart.project.service.domain_errors

object MatchStatsErrors {

  sealed trait MatchStatsError {
    def message: String
  }

  object MatchStatsError {
    final case class InvalidMatchScore(score: Int, time: String) extends MatchStatsError {
      override def message: String =
        s"Invalid match score `$score` in $time time. Match score must not be negative"
    }

    final case class EmptyMatchScore(time: String) extends MatchStatsError {
      override def message: String =
        s"Empty match score in $time time. Match score must not be empty"
    }

    final case class InvalidMatchAttendance(attendance: Int) extends MatchStatsError {
      override def message: String =
        s"Invalid match attendance `$attendance`. Match score must not be negative"
    }

    final case class MatchStatsForeignKey(id: Long) extends MatchStatsError {
      override def message: String = s"Match stats with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
