package com.itechart.project.service.domain_errors

import java.time.LocalDate

object SeasonErrors {

  sealed trait SeasonError extends DomainError

  object SeasonError {
    final case class InvalidSeasonName(name: String) extends SeasonError {
      override def message: String =
        s"Invalid season name `$name`. Season name must match regular expression: [0-9]{4}/[0-9]{4}"
    }

    final case class DuplicateSeasonName(name: String) extends SeasonError {
      override def message: String =
        s"Duplicate season name `$name`. Season name must be unique"
    }

    final case class InvalidSeasonStartDate(date: LocalDate) extends SeasonError {
      override def message: String =
        s"Invalid season start date `${date.toString}`. Year must be greater than 1900 and less than ${LocalDate.now().getYear + 1}"
    }

    final case class InvalidSeasonEndDate(date: LocalDate) extends SeasonError {
      override def message: String =
        s"Invalid season end date `${date.toString}`. Year value must be greater than start date on 1 year"
    }

    final case class SeasonForeignKey(id: Int) extends SeasonError {
      override def message: String =
        s"Season with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
