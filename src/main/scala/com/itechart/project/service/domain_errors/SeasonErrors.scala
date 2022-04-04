package com.itechart.project.service.domain_errors

import java.sql.Date

object SeasonErrors {

  sealed trait SeasonError {
    def message: String
  }

  object SeasonError {
    final case class InvalidSeasonName(name: String) extends SeasonError {
      override def message: String =
        s"Invalid season name `$name`. Season name must match regular expression: [0-9]{4}/[0-9]{4}"
    }

    final case class DuplicateSeasonName(name: String) extends SeasonError {
      override def message: String = s"Duplicate season name `$name`. Season name must be unique"
    }

    final case class InvalidSeasonStartDate(date: Date) extends SeasonError {
      override def message: String =
        s"Invalid season start date `${date.toString}`. Year must be greater than 1900 and less than ${new Date(System.currentTimeMillis()).toLocalDate.getYear + 1}"
    }

    final case class InvalidSeasonEndDate(date: Date) extends SeasonError {
      override def message: String =
        s"Invalid season end date `${date.toString}`. Year value must be greater than start date on 1 year"
    }

    final case class SeasonForeignKey(id: Int) extends SeasonError {
      override def message: String = s"Season with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
